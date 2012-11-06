
package guestbook.server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.Scanner;
import java.util.Vector;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import java.sql.SQLException;
import javax.swing.JLabel;

import guestbook.shared.*;

/** 
 *  A ClientHandler deals with incoming service requests to the server.
 *  Valid requests are of the type ClientRequest when read from the
 *  client's input stream.
 *  The server will send valid responses of type ServerResponse.
 *  For each request, the handler determines the type of request,
 *  and performs the appropriate query on the database.
 * 
 * @author Joram Benham, 200801274
 * @version April 8, 2011
 */
public class ClientHandler implements Runnable {
    private Socket client;
    private RealDatabaseHandler db;
    private ObjectOutputStream clientOut;
    private ObjectInputStream clientIn;

    /** 
     * @param theClient client
     * @param dbHandler access to database
     * @throws IOException if there is a problem connecting to the client
     */
    public ClientHandler(Socket theClient, RealDatabaseHandler dbHandler) throws IOException {
        client = theClient;
        db = dbHandler;

        try {
            clientIn = new ObjectInputStream( client.getInputStream() );
        }
        catch (IOException inputEx) {
            disconnect();
            throw new IOException("NETWORK ERROR: Could not get client's input stream.\n" + inputEx);
        }        
               
        try {
            clientOut = new ObjectOutputStream( client.getOutputStream() );
        }
        catch (IOException outputEx) {
            disconnect();
            throw new IOException("NETWORK ERROR: Could not get client's output stream.\n" + outputEx);
        }
    }

    /** 
     * Closes the client socket.
     */
    private void disconnect() {
        try { client.close(); }
        catch(IOException ioEx) {}
    }
    
    /** 
     *  Attempts to service the client request based on the type of ClientRequest
     */
    public void run() {
        if (client.isClosed()) return;
        
        // Get the request from the client
        ClientRequest request;        
        try {
            request = NetUtils.getRequest(clientIn);
        }
        catch (IOException ioGetEx) {
            // Send an error message to the client
            sendErrorMessage(ioGetEx);
            return;
        }
        
        // Service the request
        ServerResponse response;
        try {
            response = serviceRequest( request );
        }
        catch( Exception ex ) {
            // Send an error message to the client
            sendErrorMessage(ex);
            return;
        }
        
        // Send the response
        try {
            NetUtils.sendResponse(response, clientOut);
        }
        catch (IOException ioSendEx) {} // Do nothing
        
        // Disconnect
        disconnect();        
    }

    /** 
     *  Performs a different method based on the type of ClientRequest.
     * @param r the request
     * @return ServerResponse the configured response
     */
    private ServerResponse serviceRequest( ClientRequest r ) {
        String type = r.getType();
        Object[] info = r.getData();        
        ServerResponse response = null;
        
        try {
            
            if ( type.equals(NetUtils.CR_LOGIN) ) response = doLogin( info );    
            else if ( type.equals(NetUtils.CR_REGISTER) ) response = doRegister( info );  
            else if ( type.equals(NetUtils.CR_GUEST_UPDATE) ) response = doGuestUpdate( info );
            else if ( type.equals(NetUtils.CR_GUEST_RETRIEVE) ) response = doGuestRetrieve( info );
            else if ( type.equals(NetUtils.CR_GUEST_DELETE) ) response = doGuestDelete( info );
            else if ( type.equals(NetUtils.CR_SUBMIT_COMMENT) ) response = doSubmitComment( info );
            else if ( type.equals(NetUtils.CR_GET_ENTRIES) ) response = doGetEntries();
            else if ( type.equals(NetUtils.CR_GET_LOGS) ) response = doGetLogs();
            else if ( type.equals(NetUtils.CR_GET_USERS) ) response = doGetUsers( info );
            
        }
        catch (SQLException sqlEx) {
            response = new ServerResponse(NetUtils.SR_DB_ERROR, sqlEx); 
        }
        
        return response;
    }

    /** 
     *  Sends an error message to the client.
     *  @param ex the exception causing the problem
     */
    private void sendErrorMessage( Exception ex ) {
        ServerResponse errorMsg = new ServerResponse(NetUtils.SR_DB_ERROR, ex);
        try { 
            NetUtils.sendResponse(errorMsg, clientOut); 
        }
        catch (IOException ioEx) { 
            disconnect(); 
        }
    }
    
    /** 
     * @param info the data from the ClientRequest
     * @return ServerResponse with an array of users matching the criteria in the
     * ClientRequest
     * @throws SQLException if there is a database error
     */
    private ServerResponse doGetUsers(Object[] info) throws SQLException {
        GuestInfo criteria = (GuestInfo)(info[0]);       
        ArrayList<GuestInfo> users = db.retrieveUsers( criteria );
        return new ServerResponse( NetUtils.SR_GET_USERS, users.toArray() );
    }

    /** 
     * @return ServerResponse with an array of all records in the GuestLog
     * database table
     * @throws SQLException if there is a database error
     */
    private ServerResponse doGetLogs() throws SQLException {
        ArrayList<GuestLog> logs = db.getGuestLogs();
        return new ServerResponse( NetUtils.SR_GET_LOGS, logs.toArray() );
    }

    /** 
     * @return ServerResponse with an array of entries from the GuestEntry table (i.e.
     * these are entries in the guest book).
     * @throws SQLException if there is a database error
     */
    private ServerResponse doGetEntries() throws SQLException {
        ArrayList<GuestEntry> entries = db.getGuestEntries();
        return new ServerResponse( NetUtils.SR_GET_ENTRIES, entries.toArray() );
    }
  
    /** 
     * @param info the data from the ClientRequest - the comment to submit
     * @return ServerResponse indicating the operation's success.
     * @throws SQLException if there is a database error
     */
    private ServerResponse doSubmitComment(Object[] info) throws SQLException {
        GuestEntry comment = (GuestEntry)(info[0]);
        long nextGEID = db.getMaxGEID() + 1;
        comment.setGEID( nextGEID );
        
        db.addGuestEntry(comment);
        return new ServerResponse( NetUtils.SR_SUBMIT_COMMENT, new Boolean[]{true} );        
    }

    /** 
     * @param info the data from the ClientRequest - the Guest to delete
     * @return ServerResponse indicating the operation's success.
     * @throws SQLException if there is a database error
     */
    private ServerResponse doGuestDelete(Object[] info) throws SQLException {
        GuestInfo target = (GuestInfo)(info[0]);
        Boolean deleteSuccess = false;
        
        if ( db.userExists(target.getEmail()) ) {
            db.deleteRecord(target);
            deleteSuccess = true;
        }
        else deleteSuccess = false;       
        
        return new ServerResponse( NetUtils.SR_GUEST_DELETE, new Boolean[]{ deleteSuccess } );
    }
    
    /** 
     * @param info the data from the ClientRequest - the guest to get information on
     * @return ServerResponse indicating the operation's success, and the retrieved GuestInfo
     * @throws SQLException if there is a database error
     */
    private ServerResponse doGuestRetrieve(Object[] info) throws SQLException {       
        GuestInfo target = (GuestInfo)(info[0]);
        Boolean retrieveSuccess = false;
        
        if ( db.userExists(target.getEmail()) ) {
            target = db.retrieveRecord( target );
            retrieveSuccess = true;
        }
        else retrieveSuccess = false;

        return new ServerResponse( NetUtils.SR_GUEST_RETRIEVE, new Object[]{ retrieveSuccess, target } );
    }

    /** 
     * @param info the data from the ClientRequest - the update GuestInfo to submit
     * @return ServerResponse indicating the operation's success.
     * @throws SQLException if there is a database error
     */
    private ServerResponse doGuestUpdate(Object[] info) throws SQLException {
        String email = ((GuestInfo)(info[0])).getEmail();
        Boolean updateSuccess;
        
        if ( db.userExists(email) ) {
            GuestInfo updatedGuest = (GuestInfo)(info[0]);
            db.updateRecord(updatedGuest);
            updateSuccess = true;
        }
        else updateSuccess = false;
        
        return new ServerResponse( NetUtils.SR_GUEST_UPDATE, new Boolean[]{ updateSuccess } );
    }
 
    /** 
     * @param info the data from the ClientRequest - the new email to add
     * @return ServerResponse indicating the operation's success.
     * @throws SQLException if there is a database error
     */
    private ServerResponse doRegister(Object[] info) throws SQLException {
        String email = (String)(info[0]);
        Boolean emailExists;
        Boolean registerSuccess;
        
        emailExists = db.userExists(email);
            
        if (emailExists) registerSuccess = false;
        else {
            db.addRecord( new GuestInfo("", "", "", "", "", email, email) );
            registerSuccess = true;
        }
        
        return new ServerResponse( NetUtils.SR_REGISTER, new Boolean[]{registerSuccess} );
    }
  
    /** 
     * @param info the data from the ClientRequest - the email and password to check
     * @return ServerResponse indicating the operation's success.
     * @throws SQLException if there is a database error
     */
    private ServerResponse doLogin(Object[] info) throws SQLException {
        String email = (String)(info[0]);
        String pass = (String)(info[1]);
        Boolean validLogin = false;
        Boolean isAdmin = false;
        
        validLogin = db.validLogin(email, pass);
        isAdmin = db.adminExists(email);
        
        if (validLogin) {
            long nextGLID = db.getMaxGLID() + 1;
            GuestLog newLog = new GuestLog( nextGLID, email, client.getInetAddress().getHostName(), new GregorianCalendar() );
            db.addGuestLog( newLog );
        }
        
        return new ServerResponse(NetUtils.SR_LOGIN, new Boolean[]{validLogin, isAdmin});        
    }   
    
}
