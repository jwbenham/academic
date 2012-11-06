package guestbook.shared;
import java.io.Serializable;

/**
 *  Stores record information on a guest including:
 *  Name, address, city, postcode, telephone, email, and password
 *              
 *  @author Joram Benham 200801274
 *  @version April 6, 2011
 */
public class GuestInfo implements Serializable
{
    private static final long serialVersionUID = 2011040602; // yyyy/mm/dd/version
    private final String id;
    
    // fields for the details of the GuestInfo
    private String name;
    private String address; 
    private String city; 
    private String postcode; 
    private String telephone; 
    private String email;
    private String password;
    
    /** 
     *  Constructor which takes just the Email parameter
     *  @param e the email
     *  @throws IllegalArgumentException 
     */
    public GuestInfo(String e) throws IllegalArgumentException {
        this(null, null, null, null, null, e, null);
    }

    /**
     * Email and password Constructor
     * @param e the email
     * @param p the password
     */
    public GuestInfo(String e, String p) {
        this(null, null, null, null, null, e, p);
    }
    
    /**
     * Constructor with no password
     * @param n the name
     * @param a the address
     * @param c the city
     * @param p the postcode
     * @param tel the telephone number
     * @param e the email
     * @throws IllegalArgumentException 
     */
    public GuestInfo(String n, String a, String c, String p,
                   String tel, String e) throws IllegalArgumentException {
        
        this(n, a, c, p, tel, e, "password");
    }
    
    /**
     * Constructor with all data fields
     * @param n the name
     * @param a the address
     * @param c the city
     * @param p the postcode
     * @param tel the telephone number
     * @param e the email
     * @param pass the password
     * @throws IllegalArgumentException 
     */
    public GuestInfo(String n, String a, String c, String p,
                   String tel, String e, String pass) throws IllegalArgumentException {
        
        setName(n);
        setAddress(a);
        setCity(c); 
        setPostcode(p);
        setTelephone(tel);  
        setEmail(e);
        setPassword(pass);
        
        id = email;
    }
    
    /**
     * print out this contact's details prettily 
     */
    public void show() { System.out.println(this.toString()); }
   
    /** 
     *  Compute a pretty string representation of this GuestInfo's details
     *  @return String
     */
     public String toString() {
        String s=""; 
        s += "Name: \t" + name + "\n"; 
        s += "Address: \t" + address + "\n"; 
        s += "\t" + city + "\n"; 
        s += "\t" + postcode + "\n"; 
        s += "Tel: \t" + telephone + "\n"; 
        s += "Email: \t" + email + "\n";
        s += "Password: \t" + password + "\n";
        return s; 
     }
     
     /**
      * Returns TRUE if two GuestInfo objects have the same name.
      * @param rhs "right-hand side", the contact being compared against
      * @return boolean
      */
     public boolean equals(Object rhs) {
         return ( this.id.equals( ((GuestInfo)rhs).id) );
     }
     
     /**
      * Returns TRUE if a string is not null, and is not empty.
      * @param target the String to check
      * @return boolean
      */
     protected boolean hasValue(String target) {
        if (target == null) return false;
        if (target.equals("")) return false;
        return true;
     }
     
     /**
      * Sets the name member if the argument is not null/empty.
      * A name may contain: letters, -, ', and spaces.
      * @param aName the candidate name
      * @throws IllegalArgumentException 
      */
     public void setName(String aName) throws IllegalArgumentException {
         if ( hasValue(aName) ) {
            if ( aName.matches("[a-zA-z'\\-\\ ]+") ) {
                name = new String(aName);
            } else  throw new IllegalArgumentException("Invalid name value");
        } else name = "";
     }
     
     /**
      *     Sets the address if the argument is not null/empty and if it
      * contains only the following: digits, letters, -, ', or spaces.
      * @param anAddress the candidate address value
      * @throws IllegalArgumentException 
      */
     public void setAddress(String anAddress) throws IllegalArgumentException {
        if ( hasValue(anAddress) ) {
            if ( anAddress.matches("[0-9a-zA-Z\\-\\'\\ ]+") ) {
                address = new String(anAddress);
            } else throw new IllegalArgumentException("Invalid address value");
        } else address = "";
     }
     
     /**
      *     Sets the city member if the argument is not null/empty.
      * A city may contain: letters, -, ', and spaces.
      * @param aCity the candidate city value  
      * @throws IllegalArgumentException 
      */
     public void setCity(String aCity) throws IllegalArgumentException {
        if ( hasValue(aCity) ) {
            if (aCity.matches("[a-zA-z'\\-\\ ]+")) city = new String(aCity); 
            else throw new IllegalArgumentException("Invalid city value");
        } else city = "";
     }
  
     /**
      *     Sets the postcode member if the argument is not null/empty and is in
      * the following form:
      *  [letter][digit][letter][optional space][digit][letter][digit]
      * @param aPostcode the candidate postcode value  
      * @throws IllegalArgumentException 
      */
     public void setPostcode(String aPostcode) throws IllegalArgumentException {
        if ( hasValue(aPostcode) ) { 
            if (aPostcode.matches("[a-zA-Z][\\d][a-zA-Z][\\ ]?[\\d][a-zA-Z][\\d]")){
                postcode = new String(aPostcode);
            } else throw new IllegalArgumentException("Invalid postcode value");
        } else postcode = "";
     }
     
     /**
      *     Sets the telephone member if the argument is not null/empty.
      * A telephone number may contain: digits, -, (, )
      * @param aNumber the candidate phone number value
      * @throws IllegalArgumentException 
      */
     public void setTelephone(String aNumber) throws IllegalArgumentException {
        if ( hasValue(aNumber) ) {
            if (aNumber.matches("[\\d\\-\\(\\)\\ ]+")) telephone = new String(aNumber);
            else throw new IllegalArgumentException("Invalid phone number value");
        } else telephone = "";
     }
     
     /**
      *     Sets the email member if the argument is not null/empty.
      * An email should be in the following format (simplified):
      * [non-whitespace][@][non-whitespace].[non-whitespace]
      * @param anEmail the candidate email value
      * @throws IllegalArgumentException 
      */
     public void setEmail(String anEmail) throws IllegalArgumentException {
        if ( hasValue(anEmail) && anEmail.matches("[\\S]+@[\\S]+\\.[\\S]+") ) {
                email = new String(anEmail);
        }
        else throw new IllegalArgumentException("Invalid email value");
     }
     
     /**
      *     Sets the password member if the argument is not null/empty.
      * A password can be any combination of characters.
      * @param pass the password value
      * @throws IllegalArgumentException 
      */
     public void setPassword(String pass) {
        if ( hasValue(pass) ) {
                password = new String(pass);
        } else password = "";
     }
     
    /** 
     * @return String the ID
     */
    public String getID() { return id; }
    
    /** 
     * @return String the name
     */
    public String getName() { return new String(name); }
    
    /** 
     * @return String the address
     */
    public String getAddress() { return address; }
    
    /** 
     * @return String the city
     */
    public String getCity() { return city; }
    
    /** 
     * @return String the postcode
     */
    public String getPostcode() { return postcode; }
    
     /** 
     * @return String the telephone number
     */
    public String getTelephone() { return telephone; }
    
    /** 
     * @return String the email address
     */
    public String getEmail() { return email; }
    
    /** 
     * @return String the password
     */
    public String getPassword() { return password; }
}
