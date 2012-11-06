
package guestbook.client;

import guestbook.shared.*;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * A GuestBookClient provides a program with an interface to
 * connect to a server hosting a guest book database.
 * The GuestBookClient creates and sends requests for information to
 * the server, and deals with any responses or errors that occur.
 *              
 *  @author Joram Benham 200801274
 *  @version April 8, 2011
 */
public class GuestBookClient {
    private ObjectInputStream serverIn;     // used to read server messages
    private ObjectOutputStream serverOut;   // used to send messages to the server

    private Socket server;  // connection socket
    private String host;    // address of the server
    private int port;       // port the server is listening on
    
    private boolean connected;  // indicates if connection to server is successful
    
    /** 
     *  Intially the client is not connected. The setDetails method should be called before
     *  a connection is made.
     */
    public GuestBookClient() {
       connected = false;
    }
    
    /** 
     *  Sets the connection details: i.e. the address and port of the server
     *  @param theHost address for the server
     *  @param thePort port # the server is on
     *  @throws IllegalArgumentException
     */
    public void setDetails(String theHost, String thePort) throws IllegalArgumentException {
        try {
            int portNum = Integer.parseInt( thePort );
            setDetails(theHost, portNum);
        }
        catch ( NumberFormatException formatEx ) {
            throw new IllegalArgumentException("Port number must be an integer.");
        }
    }
    
    /** setDetails with port as int
     *  Sets the connection details: i.e. the address and port of the server
     *  @param theHost address for the server
     *  @param thePort port # the server is on
     *  @throws IllegalArgumentException
     */
    public void setDetails(String theHost, int thePort) throws IllegalArgumentException {
        
        if ( GUIUtils.isNullOrEmpty(theHost) ) {
            throw new IllegalArgumentException("IP Address cannot be null.");
        }
        host = theHost;        
        
        if (thePort < 0 || thePort > 65535) {
            throw new IllegalArgumentException("Port number must be between 0 and 65535");
        }
        port = thePort;
    }
    
    /** 
     *  Attempts to open a socket with the server, and open input/output streams to the server.
     *  @throws IOException if there is a network error
     */
    public void connect() throws IOException {
        try { 
            server = new Socket(host, port); 
        }
        catch (IOException socketEx) {
            throw new IOException("NETWORK ERROR: Could not establish connection to server.\n" + socketEx);
        }

        try {            
            serverOut = new ObjectOutputStream( server.getOutputStream() );
        }
        catch (IOException outputEx) {
            throw new IOException("NETWORK ERROR: Could not get server's output stream.\n" + outputEx);
        }

        try {
            serverIn = new ObjectInputStream( server.getInputStream() );
        }
        catch (IOException inputEx) {
            throw new IOException("NETWORK ERROR: Could not get server's input stream.\n" + inputEx);
        }
        
        connected = true;
    }
    
    /** 
     *  Attempts to close the connection socket with the server.
     */
    public void disconnect() {
        try { server.close(); }
        catch (IOException ioEx) {}
        connected = false;
    }
    
    /** 
     *  @return boolean true if the client is connected
     */
    public boolean isConnected() { return connected; }
    
    /**
     *  Uses shared.NetUtils to send a request to the server, and to read the
     *  server's response.
     *  @param request the request to send
     *  @return ServerResponse the server's response
     *  @throws IOException if there is a network error
     */
    public ServerResponse getRequestResponse( ClientRequest request ) throws IOException {
        connect();
        
        try {
            NetUtils.sendRequest( request, serverOut );
        }
        catch (IOException ioSendEx) { 
            throw new IOException("NETWORK ERROR: failed to send client request.\n" + ioSendEx );
        }
        
        ServerResponse response;
        try {
            response = NetUtils.getResponse( serverIn );
            return response;
        }
        catch (IOException ioGetEx) {
            ioGetEx.printStackTrace();
            throw new IOException("NETWORK ERROR: failed to retrieve server response.\n" + ioGetEx );
            
        }
        
        finally {
            disconnect();
        }
    }
    
    /** 
     *  Sends a login request to the server. The return value indicates the login success/failure:
     *  - 0 indicates the login failed
     *  - 1 indicates the login succeeded and the user is a regular user
     *  - 2 indicates the login succeeded and the user is an administrator
     *  @param email the user email
     *  @param password the user password
     *  @return int indicates login status
     *  @throws IOException if there is a network error
     *  @throws Exception
     */
    public int login(String email, String password) throws IOException, Exception {  
        
        // Send the request and get the response
        ClientRequest loginRequest = new ClientRequest( NetUtils.CR_LOGIN, new String[]{email, password} );
        ServerResponse loginResponse = getRequestResponse( loginRequest );
        
        // Get the response fields
        String type = loginResponse.getType();
        Object[] replyData = loginResponse.getData();
        
        // If the response is non-error, return whether or not the login was successful
        if ( type.equals(NetUtils.SR_LOGIN) ) {
            Boolean validLogin = (Boolean)(replyData[0]);
            Boolean isAdmin = (Boolean)(replyData[1]);
            
            if ( validLogin == false ) return 0;
            if ( validLogin && !isAdmin ) return 1;
            if (validLogin && isAdmin) return 2;
        }
        
        else if ( type.equals(NetUtils.SR_DB_ERROR) ) {
            throw new Exception( loginResponse.getEx() );
        }
        
        return 0;
    }
    
    /** 
     *  Sends a register request to the server. The return value indicates the register success/failure:
     *  - false indicates the registration failed, the email already exists
     *  - true indicates the registration succeeded
     *  @param email the user email
     *  @return boolean indicates registration status
     *  @throws IOException if there is a network error
     *  @throws Exception
     */
    public boolean register(String email) throws IOException, Exception {
        
        // Send the request and get the response
        ClientRequest registerRequest = new ClientRequest( NetUtils.CR_REGISTER, new String[]{email} );
        ServerResponse registerResponse = getRequestResponse( registerRequest );
        
        // Get the response fields
        String type = registerResponse.getType();
        Object[] replyData = registerResponse.getData();
        Boolean registerSuccess = false;
        
        // If the response is non-error, return whether or not the register was successful
        if ( type.equals(NetUtils.SR_REGISTER) ) {
            registerSuccess = (Boolean)(replyData[0]);
        }
        
        else if ( type.equals(NetUtils.SR_DB_ERROR) ) {
            throw new Exception( registerResponse.getEx() );    
        }
        
        return registerSuccess;
    }

    /** 
     *  Sends a record update request to the server.
     *  @param updatedGuest contains the data to update a guest in the database
     *  @return boolean true if successful
     *  @throws IOException if there is a network error
     */
    public boolean updateRecord(GuestInfo updatedGuest) throws IOException, Exception {
        
        // Send the request and get the response
        ClientRequest updateRequest = new ClientRequest( NetUtils.CR_GUEST_UPDATE, new GuestInfo[]{ updatedGuest } );
        ServerResponse updateResponse = getRequestResponse( updateRequest );
        
        // Get the response fields
        String type = updateResponse.getType();
        Object[] reply = updateResponse.getData();
        Boolean updateSuccess = false;
        
        // If the response is non-error, return whether or not the update was successful
        if ( type.equals( NetUtils.SR_GUEST_UPDATE ) ) {
            updateSuccess = (Boolean)(reply[0]);    
        }
        
        else if ( type.equals(NetUtils.SR_DB_ERROR) ) {
            throw new Exception( updateResponse.getEx() );    
        }
        
        return updateSuccess;
    }
    
    /** 
     *  Sends a retrieval request to the server.
     *  @param target the guest to retrieve
     *  @return GuestInfo the retrieved record, null if no such record exists
     *  @throws IOException if there is a network error
     *  @throws Exception
     */
    public GuestInfo retrieveRecord(GuestInfo target) throws IOException, Exception {
        
        // Send the request and get the response
        ClientRequest retrieveRequest = new ClientRequest( NetUtils.CR_GUEST_RETRIEVE, new GuestInfo[]{ target } );
        ServerResponse retrieveResponse = getRequestResponse( retrieveRequest );
        
        // Get the response fields
        String type = retrieveResponse.getType();
        Object[] reply = retrieveResponse.getData();
        Boolean retrieveSuccess = false;
        GuestInfo retrievedGuest = null;
        
        // If the response is non-error, return the retrieved record successful
        if ( type.equals( NetUtils.SR_GUEST_RETRIEVE ) ) {
            retrieveSuccess = (Boolean)(reply[0]);   
            if (retrieveSuccess) retrievedGuest = (GuestInfo)(reply[1]);
        }
        
        else if ( type.equals(NetUtils.SR_DB_ERROR) ) {
            throw new Exception( retrieveResponse.getEx() );    
        }
        
        return retrievedGuest;
    }
    
    /** 
     *  Sends a record delete request to the server.
     *  @param target the guest to delete
     *  @return boolean true if successful
     *  @throws IOException if there is a network error
     *  @throws Exception
     */
    public boolean deleteRecord(GuestInfo target) throws IOException, Exception {
        
        // Send the request and get the response
        ClientRequest deleteRequest = new ClientRequest( NetUtils.CR_GUEST_DELETE, new GuestInfo[]{ target } );
        ServerResponse deleteResponse = getRequestResponse( deleteRequest );
        
        // Get the response fields
        String type = deleteResponse.getType();
        Object[] reply = deleteResponse.getData();
        Boolean deleted = false;
        
        // If the response is non-error, return whether or not the deletion was successful
        if ( type.equals( NetUtils.SR_GUEST_DELETE ) ) {
            deleted = (Boolean)(reply[0]);
        }
        
        else if ( type.equals(NetUtils.SR_DB_ERROR) ) {
            throw new Exception( deleteResponse.getEx() );    
        }

        return deleted;
    }
    
    /** 
     *  Sends a guest book comment submission request to the server.
     *  @param  comment the new comment to add to the guest book
     *  @return boolean true if successful
     *  @throws IOException if there is a network error
     *  @throws Exception
     */
    public boolean submitComment(GuestEntry comment) throws IOException, Exception {
        
        // Send the request and get the response
        ClientRequest submitRequest = new ClientRequest( NetUtils.CR_SUBMIT_COMMENT, new GuestEntry[]{comment} );
        ServerResponse submitResponse = getRequestResponse( submitRequest );
        
        // Get the response fields
        String type = submitResponse.getType();
        Object[] reply = submitResponse.getData();
        Boolean submitted = false;
        
        // If the response is non-error, return whether or not the submission was successful
        if ( type.equals( NetUtils.SR_SUBMIT_COMMENT ) ) {
            submitted = (Boolean)(reply[0]);
        }
        
        else if ( type.equals(NetUtils.SR_DB_ERROR) ) {
            throw new Exception( submitResponse.getEx() );    
        }

        return submitted;
    }
    
    /** 
     *  Sends a request for all guest book comment entries to the server.
     *  @return ArrayList<GuestEntry> of the entries
     *  @throws IOException if there is a network error
     *  @throws Exception
     */
    public ArrayList<GuestEntry> getEntries() throws IOException, Exception {
        
        // Send the request and get the response
        ClientRequest entriesRequest = new ClientRequest( NetUtils.CR_GET_ENTRIES, null );
        ServerResponse entriesResponse = getRequestResponse( entriesRequest );
        
        // Get the response fields
        String type = entriesResponse.getType();
        Object[] reply = entriesResponse.getData();  
        if (reply.length < 1) return null;
        ArrayList<GuestEntry> entries = null;
        
        // If the response is non-error, return the ArrayList of guest book entries
        if ( type.equals(NetUtils.SR_GET_ENTRIES) ) { 
            entries = new ArrayList<GuestEntry>();
            
            for (int i = 0; i < reply.length; i++) {
                entries.add( (GuestEntry)reply[i] );
            }
        }
        
        else if ( type.equals(NetUtils.SR_DB_ERROR) ) {
            throw new Exception( entriesResponse.getEx() );    
        }
        
        return entries;
    }
    
    /** 
     *  Sends a request for all guest book server logs to the server.
     *  @return ArrayList<GuestLog> of the logs
     *  @throws IOException if there is a network error
     *  @throws Exception
     */
    public ArrayList<GuestLog> getLogs() throws IOException, Exception {
        
        // Send the request and get the response
        ClientRequest logsRequest = new ClientRequest( NetUtils.CR_GET_LOGS, null );
        ServerResponse logsResponse = getRequestResponse( logsRequest );
        
        // Get the response fields
        String type = logsResponse.getType();
        Object[] reply = logsResponse.getData();
        if (reply.length < 1) return null;
        ArrayList<GuestLog> logs = null;
        
        // If the response is non-error, return the ArrayList of server logs 
        if ( type.equals(NetUtils.SR_GET_LOGS) ) {
            logs = new ArrayList<GuestLog>();
            
            for (int i = 0; i < reply.length; i++) {
                logs.add( (GuestLog)reply[i] );
            }
        }
        
        else if ( type.equals(NetUtils.SR_DB_ERROR) ) {
            throw new Exception( logsResponse.getEx() );    
        }
        
        return logs;
    }
    
    /** 
     *  Sends a request for all guest book guests to the server.
     *  @param criteria
     *  @return ArrayList<GuestInfo> of the guests
     *  @throws IOException if there is a network error
     *  @throws Exception
     */
    public ArrayList<GuestInfo> getUsers(GuestInfo criteria) throws IOException, Exception {
        
        // Send the request and get the response
        ClientRequest usersRequest = new ClientRequest( NetUtils.CR_GET_USERS, new GuestInfo[]{ criteria } );
        ServerResponse usersResponse = getRequestResponse( usersRequest );
        
        // Get the response fields
        String type = usersResponse.getType();
        Object[] reply = usersResponse.getData();
        if (reply.length < 1) return null;
        ArrayList<GuestInfo> users = null;
        
        // If the response is non-error, return the ArrayList of guests in the database
        if ( type.equals(NetUtils.SR_GET_USERS) ) {            
            users = new ArrayList<GuestInfo>();
            
            for (int i = 0; i < reply.length; i++) {
                users.add( (GuestInfo)reply[i] );
            }
        }
        
        else if ( type.equals(NetUtils.SR_DB_ERROR) ) {
            throw new Exception( usersResponse.getEx() );    
        }
        
        return users;
    }
}
