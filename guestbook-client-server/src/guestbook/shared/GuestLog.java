
package guestbook.shared;

import java.util.GregorianCalendar;
import java.util.Calendar;
import java.io.Serializable;

/** 
 *  Represents a record from the GuestLog table in the database.
 *  This is interpreted as server log of a user visit. It holds the ID of the log,
 *  the email of the user who generated it, user's IP, and the submission date.
 *              
 *  @author Joram Benham 200801274
 *  @version April 8, 2011
 */
public class GuestLog implements Serializable {
    private static final long serialVersionUID = 20110306; // yyyy/mm/dd
    private long GLID;
    private String email;
    private String ip;
    private String date;
    
    /**  
     *  Constructor with Calendar date argument
     *  @param id the log id
     *  @param e the user email
     *  @param ipaddress the user's ipaddress
     *  @param d Calendar date
     *  @throws IllegalArgumentException
     */
    public GuestLog(long id, String e, String ipaddress, Calendar d) throws IllegalArgumentException {
        setGLID(id);
        setEmail(e);
        setIP(ipaddress);
        setDate(d);
    }
 
    /** 
     *  Constructor with String date argument
     *  @param id the log id
     *  @param e the user email
     *  @param ipaddress the user's ipaddress
     *  @param d String date
     *  @throws IllegalArgumentException
     */
    public GuestLog(long id, String e, String ipaddress, String d) throws IllegalArgumentException {
        setGLID(id);
        setEmail(e);
        setIP(ipaddress);
        setDate(d);
    }
    
    /** 
     * @return long the GLID
     */
    public long getGLID() { return GLID; }
    
    /** 
     * @return String the email
     */
    public String getEmail() { return email; }
    
    /** 
     * @return String the IP
     */
    public String getIP() { return ip; }
    
    /** 
     * Returns the full date and time : yyyy-mm-dd hh:mm:ss
     * @return String
     */
    public String getDate() { return date; }
    
     /** 
     * Returns just the date: yyyy-mm-dd
     * @return String
     */
    public String getDateOnly() { return date.substring(0, 10); }
    
    /** 
     * Sets the GLID
     * @param id the GLID
     * @throws IllegalArgumentException
     */
    public void setGLID(long id) throws IllegalArgumentException {
        if (Long.toString(id).length() > 10) {
            throw new IllegalArgumentException("Max digits in GLID is 10.");
        }
        if ( id < 0 ) {
            throw new IllegalArgumentException("GLID cannot be < 0.");
        }
        GLID = id;
    }
    
    /** 
     * Sets the email
     * @param e the email
     * @throws IllegalArgumentException
     */
    public void setEmail(String e) throws IllegalArgumentException {
        if (e == null) throw new IllegalArgumentException("Email cannot be NULL.");
        if (e.matches("[\\S]+@[\\S]+\\.[\\S]+")) email = new String(e);
        else throw new IllegalArgumentException("Email should be in format __@__.__");
    }
    
    /** 
     * Sets the IP
     * @param ipaddress the IP
     * @throws IllegalArgumentException
     */
    public void setIP(String ipaddress) throws IllegalArgumentException {
        if (ipaddress == null) throw new IllegalArgumentException("IP cannot be NULL.");
        ip = new String(ipaddress);
    }
    
    /** 
     * Sets the date
     * @param d the date as a Calendar
     */
    public void setDate(Calendar d) {
        date = d.get(Calendar.YEAR) + "-"
             + String.valueOf(d.get(Calendar.MONTH) + 1) + "-"
             + d.get(Calendar.DAY_OF_MONTH) + " "
             + d.get(Calendar.HOUR_OF_DAY) + ":"
             + d.get(Calendar.MINUTE) + ":"
             + d.get(Calendar.SECOND);
    }
    
    /** 
     * Sets the date with a String
     * @param d the date String
     */
    public void setDate(String d) {
        date = new String(d);
    }
    
    /** 
     * Returns the GuestLog as a String
     * @return String
     */
    public String toString() {
        String str = String.valueOf(GLID) + " " 
                    + email + " "
                    + ip + " "
                    + date;
        return str;
    }
}
