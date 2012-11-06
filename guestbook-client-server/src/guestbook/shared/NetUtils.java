
package guestbook.shared;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import java.util.Arrays;

/** 
 *  A utility class used for communication between a ClientHandler
 *  and a GuestBookClient.
 *  The class defines the types of communication headers which can be
 *  sent between ClientHandler and GuestBookClient, and provides methods
 *  for sending and receiving requests and responses.
 *              
 *  @author Joram Benham 200801274
 *  @version April 8, 2011
 */
public class NetUtils {

    // Type headers for the communications between client and server
    // SR indicates "Server Response"
    // CR indicates "Client Request"
    public static final String CR_LOGIN = "Login request";
    public static final String SR_LOGIN = "Login response";
    
    public static final String CR_REGISTER = "Register request";
    public static final String SR_REGISTER = "Register response";
    
    public static final String CR_GUEST_UPDATE = "Guest update request";
    public static final String SR_GUEST_UPDATE = "Guest update response";    
    
    public static final String CR_GUEST_RETRIEVE = "Guest retrieve request";
    public static final String SR_GUEST_RETRIEVE = "Guest retrieve response"; 
    
    public static final String CR_GUEST_DELETE = "Guest delete request";
    public static final String SR_GUEST_DELETE = "Guest delete response"; 
    
    public static final String CR_SUBMIT_COMMENT = "Submit comment request";
    public static final String SR_SUBMIT_COMMENT = "Submit comment response"; 
    
    public static final String CR_GET_ENTRIES = "Get entries request";
    public static final String SR_GET_ENTRIES = "Get entries response";
    
    public static final String CR_GET_LOGS = "Get logs request";
    public static final String SR_GET_LOGS = "Get logs response";
    
    public static final String CR_GET_USERS = "Get users request";
    public static final String SR_GET_USERS = "Get users response";
    
    public static final String SR_DB_ERROR = "Database error";
    
    public static final String[] CLIENT_REQUESTS = { CR_LOGIN, CR_REGISTER, CR_GUEST_UPDATE, CR_GUEST_RETRIEVE, CR_GUEST_DELETE, CR_SUBMIT_COMMENT, CR_GET_ENTRIES, CR_GET_LOGS, CR_GET_USERS };
    public static final String[] SERVER_RESPONSES = { SR_LOGIN, SR_REGISTER, SR_GUEST_UPDATE, SR_GUEST_RETRIEVE, SR_GUEST_DELETE, SR_SUBMIT_COMMENT, SR_GET_ENTRIES, SR_GET_LOGS, SR_GET_USERS, SR_DB_ERROR };
    
    /** Determines if a String is a valid request type
     * @param r string to check for validity
     * @return boolean true if 'r' is a valid request type
     */
    public static boolean isValidRequest( String r ) {
        if ( r == null ) return false;
        if ( Arrays.asList( CLIENT_REQUESTS ).contains( r ) ) return true;
        return false;
    }
    
    /** Determines if a String is a valid response type
     * @param r string to check for validity
     * @return boolean true if 'r' is a valid response type
     */
    public static boolean isValidResponse( String r ) {
        if (r == null) return false;
        if ( Arrays.asList( SERVER_RESPONSES ).contains( r ) ) return true;
        return false;
    }

    /** 
     *  Sends a request to the server.
     * @param request ClientRequest object to send to the server
     * @param out the stream to send the request with
     * @throws IOException
     */
    public static void sendRequest( ClientRequest request, ObjectOutputStream out ) throws IOException {
        out.writeObject( request );
    }
    
    /** 
     *  Sends a response to the client.
     * @param response ServerResponse object to send to the client
     * @param out the stream to send the response with
     * @throws IOException
     */
    public static void sendResponse( ServerResponse response, ObjectOutputStream out ) throws IOException {
        out.writeObject( response );
    }
    
    /** 
     *  Used by the server to get a client's request
     * @param in the stream to read the request from
     * @return ClientRequest
     * @throws IOException
     */
    public static ClientRequest getRequest( ObjectInputStream in ) throws IOException {
        Object received;
        
        try {
            received = in.readObject();
        }
        catch (ClassNotFoundException classEx) {
            throw new IOException("NETWORK ERROR: Client did not send a valid request.");
        }
        
        if ( received instanceof ClientRequest ) {
            return (ClientRequest)(received);
        }
        else {
            throw new IOException("NETWORK ERROR: Client's request was not of type ClientRequest.");
        }
    }  
    
    /** 
     *  Used by a client to get the server's response
     * @param in the stream to read the response from
     * @return ServerResponse
     * @throws IOException
     */
    public static ServerResponse getResponse( ObjectInputStream in ) throws IOException {
        Object received;
        
        try {
            received = in.readObject();
        }
        catch (ClassNotFoundException classEx) {
            throw new IOException("NETWORK ERROR: Server did not send a valid response.");
        }
        
        if ( received instanceof ServerResponse ) {
            return (ServerResponse)(received);
        }
        else {
            throw new IOException("NETWORK ERROR: Server's response was not of type ServerResponse.");
        }
    }
}
