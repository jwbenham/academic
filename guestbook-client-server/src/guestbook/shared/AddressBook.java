
package guestbook.shared;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Vector;
import java.util.Arrays;

import javax.swing.JOptionPane;

/**
 * 
 *  An AddressBook provides functionality for working with a virtual address 
 * book of guests, implemented as a vector of GuestInfo objects.
 *  Note that duplicate guests are not permitted in the address book, where
 * two guests are considered duplicates if they share the same ID.
 * 
 * @author Joram Benham, 200801274
 * @version April 8, 2011
 */
public class AddressBook implements Serializable {
        
    public static final String ADD_SUCCESS = "Add successful.";
    public static final String ADD_FAIL = "GuestInfo already exists";
    public static final String UPDATE_SUCCESS = "Update successful.";
    public static final String UPDATE_FAIL = "GuestInfo does not exist.";   
    public static final String DELETE_SUCCESS = "Delete successful.";
    public static final String DELETE_FAIL = "GuestInfo does not exist.";
    public static final String IS_EMPTY = "Address book is empty.";
        
    private static final long serialVersionUID = 20110406; // yyyy/mm/dd
    private Vector<GuestInfo> guests;   
    
    /**
     * Default Constructor
     */
    public AddressBook() { guests = new Vector<GuestInfo>(); }
    
    /**
     *  Adds a guest to the address book
     *  @param newGuestInfo the guest to add
     *  @return String indicating operation status
     */
    public String addGuestInfo(GuestInfo newGuestInfo) {        
        if ( guests.contains( newGuestInfo ) ) return new String(ADD_FAIL);     
        guests.add( newGuestInfo );     
        return new String(ADD_SUCCESS);     
    }
    
    /**
     *  Updates a guest in the address book
     * @param updatedGuestInfo the guest to be updated
     * @return String indicating operation status
     */
    public String updateGuestInfo(GuestInfo updatedGuestInfo) {
        if (guests.isEmpty()) return IS_EMPTY;
        
        if ( guests.contains( updatedGuestInfo ) ) {
            guests.setElementAt( updatedGuestInfo, guests.indexOf(updatedGuestInfo) );
            return new String(UPDATE_SUCCESS);
        } 
        
        return new String(UPDATE_FAIL);
    }
    
    /**
     *  Deletes a guest from the address book.
     * @param deletedGuestInfo the guest to be deleted
     * @return String indicating operation status
     */
    public String deleteGuestInfo(GuestInfo deletedGuestInfo) {
        if (guests.isEmpty()) return IS_EMPTY;      
        if ( guests.remove(deletedGuestInfo) ) return new String(DELETE_SUCCESS);       
        return new String(DELETE_FAIL);
    }
    
    /** 
     *  Takes a guest name as input and returns a reference to the GuestInfo
     * @param target GuestInfo to view
     * @return GuestInfo of requested record
     */
    public GuestInfo viewGuestInfo(GuestInfo target) {
        if (target != null) {
            for (GuestInfo c : guests) if ( c.equals(target) ) return c;
        }
        
        return null;
    }
    
    /**
     *  Returns the number of guests in the address book
     *  @return int the size of the AddressBook
     */
    public int size() { return guests.size(); }
    
    /**
     *  Returns the guests in the AddressBook in String format.
     *  @return String
     */
    public String toString() {
        String bookAsString = "";       
        for (GuestInfo c : guests) { bookAsString += c + "\n"; }        
        return bookAsString;
    }
    
    /**
     * Returns an array of all names in the AddressBook
     * @return String[]
     */
    public String[] getNames() {
        String[] names = null;
        
        if ( !guests.isEmpty() ) {
            names = new String[ guests.size() ];
        
            for (int i = 0; i < names.length; i++) {
                names[i] = guests.elementAt(i).getName();
            }
        
            Arrays.sort( names );   
        }
        
        return names;
    }
    
    /**
     *  Writes an address book out as a binary file using ObjectOutputStream.
     * @param saveFile the file to save to
     * @throws IOException for file not found, or error with output stream
     */
    public void save(File saveFile) throws IOException {
        ObjectOutputStream objectOut = null;        
        objectOut = new ObjectOutputStream( new FileOutputStream( saveFile ) );

        objectOut.writeObject( this );
        
        objectOut.close();
    }
    
    /**
     *  Loads an address book from a binary file and assigns its GuestInfo Vector
     * to this address book.
     * @param loadFile the file to load the address book from
     * @throws IOException for file not found, or error with input stream
     * @throws ClassNotFoundException for bad input file
     */
    public void load(File loadFile) throws IOException, ClassNotFoundException {
        ObjectInputStream objectIn = null;
        objectIn = new ObjectInputStream( new FileInputStream( loadFile ) );
        
        AddressBook loadedBook = (AddressBook)objectIn.readObject();
        
        objectIn.close();
    
        this.guests = loadedBook.guests;
    }

    
    /**
     *  Imports either new guests from an existing text file and adds them
     * (if they are unique) to this AddressBook.
     * @param importFile the file to load the new guests from
     * @param recordDelimRegex the regex to split the file contents on, to get
     *                          the separate guest records
     * @param fieldDelimRegex the regex to split the records on, to get the 
     *                          separate fields for a guest
     */
    public void importGuestInfos(File importFile, String recordDelimRegex, String fieldDelimRegex) throws IOException {
        BufferedReader br = null;
        br = new BufferedReader( new FileReader( importFile ) );
        
        // If the record delimiter is a line terminator, it needs to be added to
        // the end of each line, as readLine() will not return it.
        String endline = "";
        if ( recordDelimRegex.matches( "\\n|\\r|\\r\\n" ) ) endline = recordDelimRegex;;
        
        // Read the file contents in.
        String contents = "";
        String line = ""; 
        
        while ( line != null ) {
            contents += line + endline; // endline will be "" if the record
            line =  br.readLine();      // delimiter is not a line terminator
        }
        
        br.close(); // Close the BufferedReader.

        String[] records, fields;
        
        // Add the new guests to the address book
        if (contents != "") {           
            records = contents.split( recordDelimRegex );
            
            for (String aRecord : records) {
                fields = aRecord.split( fieldDelimRegex );
                
                // If there are 7 fields, create a new GuestInfo using the fields
                if (fields.length == 7) {
                    GuestInfo aGuestInfo;
                    aGuestInfo = new GuestInfo( fields[0], 
                                                fields[1], 
                                                fields[2],
                                                fields[3], 
                                                fields[4], 
                                                fields[5],
                                                fields[6] );
                
                    // Try to add the guest to this address book
                    this.addGuestInfo( aGuestInfo );
                }
            }
        } 
        // End adding the guests        
    }
            

}
