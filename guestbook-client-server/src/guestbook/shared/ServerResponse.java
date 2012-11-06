
package guestbook.shared;

import java.io.Serializable;

/** 
 *  Forms part of the communication protocol between the ClientHandler
 *  and a client. A ServerResponse object represents the response of a ClientHandler
 *  to a GuestBookClient's request.
 *  A ServerResponse is composed of:
 *      - type: indicates the communication header, allowing the client to receive the response
 *        appropriately
 *      - data: any data the ClientHandler has to send with the given "type" value. Each "type" has
 *              specific guidelines for what data is returned
 *      - ex: an exception, if the response is an error response (SR_DB_ERROR), indicating what went wrong
 *              
 *      The types of acceptable ServerResponse types are held in the shared.NetUtils class.
 *              
 *  @author Joram Benham 200801274
 *  @version April 8, 2011
 */
public class ServerResponse implements Serializable {
    private static final long serialVersionUID = 20110306; // yyyy/mm/dd
    private String type;
    private Object[] data;
    private Exception ex;
    
    /**
     * Constructor for normal response
     * @param t the response type
     * @param d the accompanying data
     * @throws IllegalArgumentException
     */
    public ServerResponse(String t, Object[] d) throws IllegalArgumentException {
        this(t, d, null);
    }
    
    /**
     * Constructor for error response
     * @param t the response type
     * @param e the exception
     * @throws IllegalArgumentException
     */
    public ServerResponse(String t, Exception e) throws IllegalArgumentException {
        this(t, null, e);
    }
    
    /** 
     * Private constructor
     * @param t the response type
     * @param d the accompanying data, if needed
     * @param e the exception, if needed
     * @throws IllegalArgumentException
     */
    private ServerResponse(String t, Object[] d, Exception e) throws IllegalArgumentException {
        // Set the type
        setType( t );
        
        // Assign the exception if one is given
        if (e != null) ex = e;
        
        // Assign the data if some is provided
        if (d != null) {
            String boolType = "Boolean";
            String guestEntryType = "GuestEntry";
            String guestLogType = "GuestLog";
            String guestType = "GuestInfo";
            
            if ( type.equals(NetUtils.SR_LOGIN) ) setData( d, boolType, 2 );
            else if ( type.equals(NetUtils.SR_REGISTER) ) setData( d, boolType, 1 );
            else if ( type.equals(NetUtils.SR_GUEST_UPDATE) ) setData( d, boolType, 1 );
            else if ( type.equals(NetUtils.SR_GUEST_RETRIEVE) ) setMixedData( d, 1, 1 );
            else if ( type.equals(NetUtils.SR_GUEST_DELETE) ) setData( d, boolType, 1 );
            else if ( type.equals(NetUtils.SR_SUBMIT_COMMENT) ) setData( d, boolType, 1 );
            else if ( type.equals(NetUtils.SR_GET_ENTRIES) ) setData( d, guestEntryType, d.length );
            else if ( type.equals(NetUtils.SR_GET_LOGS) ) setData( d, guestLogType, d.length );
            else if ( type.equals(NetUtils.SR_GET_USERS) ) setData( d, guestType, d.length );
        }
    }
    
    /** 
     *  Returns the type for this response
     *  @return String
     */
    public String getType() {
        return type;
    }
    
    /** 
     *  Sets the type for this response, if it is valid (determined by the shared.NetUtils.isValidResponse)
     *  @param t the type
     *  @throws IllegalArgumentException
     */
    public void setType( String t ) throws IllegalArgumentException {
        if ( NetUtils.isValidResponse(t) ) {
            type = new String(t);
        }
        else {
            throw new IllegalArgumentException("Invalid response type: " + t);
        }        
    }

    /** 
     *  Returns the data for this response
     *  @return Object[]
     */
    public Object[] getData() {
        return data;
    }
    
    /** 
     *  Returns the exception for this response
     *  @return Exception
     */
    public Exception getEx() {
        return ex;
    }
    
    /** 
     *  Sets the data for this response if the data array is of one type only. The method
     *  is provided with the data that the ClientHandler WANTS to send, and the type and amount
     *  of data that SHOULD be sent with this type of response. If the type and length of the data
     *  do not match the dataType and length parameters, an error is thrown.
     *  @param replyData the data to send to the client
     *  @param dataType the data type that should be sent back for a response of this type
     *  @param length the amount of data that should be sent back for a response of this type
     *  @throws IllegalArgumentException
     */
    private void setData( Object[] replyData, String dataType, int length ) throws IllegalArgumentException {
        String lengthMsg = "PROTOCOL ERROR: Response type '" + type + "' requires " + length + " arguments.";
        String classMsg = "PROTOCOL ERROR: Response type '" + type + "' requires '" + dataType + "' arguments.";
        
        if (replyData.length != length) throw new IllegalArgumentException(lengthMsg);
        if (length > 0) {
            if ( dataType.equals( replyData[0].getClass().getSimpleName() ) == false ) throw new IllegalArgumentException(classMsg);
        }
        data = replyData;
    }
    
    /** 
     *  Sets the data for this response if the data array is of mixed (i.e. 2) types. The replyData
     *  provided by the ClientHandler should have one or more Boolean elements indicating operation status, and
     *  one or more GuestInfo elements retrieved by the ClientHandler
     *  @param replyData the data to send to the client
     *  @param num1 the number of Boolean elements
     *  @param num2 the number of GuestInfo elements
     *  @throws IllegalArgumentException
     */
    private void setMixedData( Object[] replyData, int num1, int num2 ) throws IllegalArgumentException {
        int length = num1 + num2;
        String lengthMsg = "PROTOCOL ERROR: Response type '" + type + "' requires " + length + " arguments.";
        String classMsg1 = "PROTOCOL ERROR: Response type '" + type + "' requires " + num1 + " Boolean arguments.";
        String classMsg2 = "PROTOCOL ERROR: Response type '" + type + "' requires " + num2 + " GuestInfo arguments.";
        
        if ( replyData.length != length ) throw new IllegalArgumentException(lengthMsg);
        for (int i = 0; i < num1; i++) { 
            if ( (replyData[i] instanceof Boolean) == false ) throw new IllegalArgumentException(classMsg1);
        }
        for (int i = num1; i < length; i++) { 
            if ( (replyData[i] instanceof GuestInfo) == false ) throw new IllegalArgumentException(classMsg1);
        }
        
        data = replyData;
    }
    
}
