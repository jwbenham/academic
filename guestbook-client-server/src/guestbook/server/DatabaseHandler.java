
package guestbook.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import guestbook.shared.*;

/**  
 * The DatabaseHandler interface provides generic interaction with 
 * a database of undefined type.
 *              
 *  @author Joram Benham 200801274
 *  @version April 8, 2011
 */
public interface DatabaseHandler {    
    public void addRecord(GuestInfo info) throws Exception;
    public GuestInfo retrieveRecord(GuestInfo target) throws Exception;
    public void updateRecord(GuestInfo updatedGuest) throws Exception;
    public void deleteRecord(GuestInfo target) throws Exception;
}
