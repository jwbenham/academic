
package guestbook.shared;

import java.io.Serializable;

/** 
 *  Forms part of the communication protocol between the GuestBookClient
 *  and a ClientHandler. A ClientRequest object represents the request for service from 
 *  a GuestBookClient to a ClientHandler.
 *  A ClientRequest is composed of:
 *              - type: indicates the communication header, allowing the ClientHandler to receive the response
 *                      appropriately
 *              - data: any data the GuestBookClient has to send with the given "type" value. Each "type" has
 *                      specific guidelines for what data is sent with it.
 *              
 *  The acceptable ClientRequest types are held in the shared.NetUtils class.
 *              
 *  @author Joram Benham 200801274
 *  @version April 8, 2011
 */
public class ClientRequest implements Serializable {
    private static final long serialVersionUID = 20110306; // yyyy/mm/dd
    private String type;
    private Object[] data;
    
    /** Constructor
     *  @param t the header type for this request
     *  @param d the request data needed by the server to serve the request
     *  @throws IllegalArgumentException
     */
    public ClientRequest(String t, Object[] d) throws IllegalArgumentException {
        setType( t );
        
        String stringArray = "String";
        String guestArray = "GuestInfo";
        String entryArray = "GuestEntry";
        String logArray = "GuestLog";
        
        if ( type.equals(NetUtils.CR_LOGIN) ) setData( d, stringArray, 2 );
        else if ( type.equals(NetUtils.CR_REGISTER) ) setData( d, stringArray, 1 );
        else if ( type.equals(NetUtils.CR_GUEST_UPDATE) ) setData( d, guestArray, 1 );
        else if ( type.equals(NetUtils.CR_GUEST_RETRIEVE) ) setData( d, guestArray, 1 );
        else if ( type.equals(NetUtils.CR_GUEST_DELETE) ) setData( d, guestArray, 1 );
        else if ( type.equals(NetUtils.CR_SUBMIT_COMMENT) ) setData( d, entryArray, 1 );
        else if ( type.equals(NetUtils.CR_GET_ENTRIES) ) data = null;
        else if ( type.equals(NetUtils.CR_GET_LOGS) ) data = null;
        else if ( type.equals(NetUtils.CR_GET_USERS) ) setData( d, guestArray, 1 );
    }    
    
    /** 
     *  Sets the type if it is a valid request type as determined in
     *  shared.NetUtils.isValidRequest()
     *  @param t the header type for this request
     *  @throws IllegalArgumentException
     */
    public void setType( String t ) throws IllegalArgumentException {
        if (NetUtils.isValidRequest(t)) {
            type = new String(t);
        }
        else {
            throw new IllegalArgumentException("Invalid request type: " + t);
        }
    }
    
    /** 
     * Returns the request's type.
     * @return String
     */
    public String getType() {
        return type;
    }
    
    /** 
     * Returns the request's data.
     * @return Object[]
     */
    public Object[] getData() {
        return data;
    }   
    
    /** 
     *  Sets the data member for this request  if the provided data array matches the parameters
     *  specifying the data type and amount for this kind of request
     *  @param theData the data to be sent with the request - should match dataType and length
     *  @param dataType the data type of the data that should be sent for this request
     *  @param length the amount of data that should be sent for this request
     *  @throws IllegalArgumentException
     */
    private void setData( Object[] theData, String dataType, int length ) throws IllegalArgumentException {
        String lengthError = "PROTOCOL ERROR: Request '" + type + "' requires " + length + " argument(s).";
        String classError = "PROTOCOL ERROR: Request '" + type + "' requires '" + dataType + "' data.";        
        
        if ( theData.length != length ) throw new IllegalArgumentException(lengthError);
        if ( dataType.equals( theData[0].getClass().getSimpleName() ) == false ) throw new IllegalArgumentException(classError);
        
        data = theData;
    }  
    
}
