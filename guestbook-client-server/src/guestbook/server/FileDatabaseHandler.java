package guestbook.server;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.File;

import guestbook.shared.*;

/** 
 *  The FileDatabaseHandler class allows a user to interface with an AddressBook of
 *  GuestInfo records stored in a data file. Methods to add, remove, view, and update
 *  records are provided, implemented via the methods of the AddressBook class.
 *              
 *  @author Joram Benham 200801274
 *  @version April 8, 2011
 */
public class FileDatabaseHandler implements DatabaseHandler {

    private final File DATA_FILE;    
    private AddressBook dataSource;
    
    /** 
     *  Constructor
     *  @param dataFile the file containing the address book with the guest records
     */
    public FileDatabaseHandler(File dataFile) throws IOException, ClassNotFoundException, NullPointerException, IllegalArgumentException {
        if (dataFile == null) throw new NullPointerException("FileDatabaseHandler constructor parameter is null");
        if (dataFile.exists() == false) throw new IllegalArgumentException("FileDatabaseHandler constructor parameter file does not exist");
        
        DATA_FILE = dataFile;        
        dataSource = new AddressBook();
        
        try {
            dataSource.load(DATA_FILE);
        }
        
        catch (EOFException eofEx) {
            // Write out the initial new AddressBook if the data file is empty
            dataSource.save(DATA_FILE);
        }
    }

    /** 
     *  Adds a record to the file database.
     *  @param info the new info for a guest
     */
    public synchronized void addRecord(GuestInfo info) throws IOException, ClassNotFoundException {
        dataSource.addGuestInfo(info);
        dataSource.save(DATA_FILE);
    }
    
    /** 
     *  Retrieves a record from the file database.
     *  @param target a dummy GuestInfo object containing the email address of the record to retrieve.
     */
    public GuestInfo retrieveRecord(GuestInfo target) throws IOException, ClassNotFoundException {
        return dataSource.viewGuestInfo( target );
    }
    
    /** 
     *  Updates a record in the file database.
     *  @param updatedGuest a guest with updated info, but with the same email address as an existing record.
     */
    public synchronized void updateRecord(GuestInfo updatedGuest) throws IOException {
        dataSource.updateGuestInfo(updatedGuest);
        dataSource.save(DATA_FILE);
    }
    
    /** 
     *  Deletes a record from the file database.
     *  @param target a dummy GuestInfo object containing the email address of the record to delete.
     */
    public synchronized void deleteRecord(GuestInfo target) throws IOException {
        dataSource.deleteGuestInfo(target);
        dataSource.save(DATA_FILE);
    }
    

}
