
package guestbook.shared;

import java.util.GregorianCalendar;
import java.util.Calendar;
import java.io.Serializable;


/** 
 *  Represents a record from the GuestEntry table in the database.
 *  This is interpreted as a guest book comment. It holds the ID of the entry,
 *  the email of the user who submitted it, the entry itself, and the submission date.
 *              
 *  @author Joram Benham 200801274
 *  @version April 8, 2011
 */
public class GuestEntry implements Serializable {
    private static final long serialVersionUID = 20110306; // yyyy/mm/dd
    private long GEID;
    private String email;
    private String entry;
    private String date;
    
    /** 
     * Constructor which takes a Calendar for the date parameter
     * @param id the entry id
     * @param e the user's email
     * @param comment the comment
     * @param d the date
     * @throws IllegalArgumentException
     */
    public GuestEntry(long id, String e, String comment, Calendar d) throws IllegalArgumentException {
        setGEID(id);
        setEmail(e);
        setEntry(comment);
        setDate(d);
    }

    /** 
     * Constructor which takes a String for the date parameter
     * @param id the entry id
     * @param e the user's email
     * @param comment the comment
     * @param d the date
     * @throws IllegalArgumentException
     */
    public GuestEntry(long id, String e, String comment, String d) throws IllegalArgumentException {
        setGEID(id);
        setEmail(e);
        setEntry(comment);
        setDate(d);
    }

    /** 
     * Returns the entry ID
     * @return long
     */
    public long getGEID() { return GEID; }
    
    /**
     * Returns the entry email
     * @return String
     */
    public String getEmail() { return email; }
    
    /** 
     * Returns the entry field
     * @return String
     */
    public String getEntry() { return entry; }
    
    /**
     * Returns the entry date
     * @return String
     */
    public String getDate() { return date; }

    /** 
     * Sets entry ID
     * @param id the GEID
     * @throws IllegalArgumentException
     */
    public void setGEID(long id) throws IllegalArgumentException {
        if (Long.toString(id).length() > 10) {
            throw new IllegalArgumentException("Max digits in GEID is 10.");
        }
        if ( id < 0 ) {
            throw new IllegalArgumentException("GEID cannot be < 0.");
        }
        GEID = id;
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
     * Sets the entry field
     * @param comment the text to set the entry to
     * @throws IllegalArgumentException
     */
    public void setEntry(String comment) throws IllegalArgumentException {
        if (comment == null) throw new IllegalArgumentException("Entry cannot be NULL.");
        if (comment.length() > 4000) throw new IllegalArgumentException("Entry length cannot be > 4000.");
        entry = new String(comment);
    }
    
    /** 
     * Sets the date field with a Calendar argument
     * @param d Calendar for the date
     */
    public void setDate(Calendar d) {
        if (d != null) {
            date = d.get(Calendar.YEAR) + "-"
                + d.get(Calendar.MONTH) + "-"
                + d.get(Calendar.DAY_OF_MONTH) + " "
                + d.get(Calendar.HOUR_OF_DAY) + ":"
                + d.get(Calendar.MINUTE) + ":"
                + d.get(Calendar.SECOND);
        } else date = "";
    }

    /** 
     *  Sets the date field with a formatted date String
     *  @param d String for the date
     */
    public void setDate(String d) {
        if (d != null) date = new String(d);
        else date = "";
    }
    
    /** 
     * Returns the GuestEntry in String format
     * @return String
     */
    public String toString() {
        String str = "Entry ID: " + String.valueOf(GEID) + "\n" 
                    + "Email: " + email + "\n"
                    + "Date: " + date + "\n"
                    + "Entry: \n" + entry;
        return str;
    }   

}
