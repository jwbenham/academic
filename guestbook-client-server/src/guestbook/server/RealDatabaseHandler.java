
package guestbook.server;

import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import java.util.Vector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;

import guestbook.shared.*;


/** 
 *  The RealDatabaseHandler class allows a user to interface with an Oracle 
 *  database containg GuestInfo records.
 *              
 *  @author Joram Benham 200801274
 *  @version April 6, 2011
 */
public class RealDatabaseHandler implements DatabaseHandler {
    public static final String DUMMY_EMAIL = "x@x.x";

    private Connection guestConn;
    private Statement statement;
    private ResultSet results;
    private boolean isOpen;
    
    private String url;
    private String user;
    private String password;

    /** 
     * Loads the driver.
     * @throws ClassNotFoundException if the driver is not found
     */
    public RealDatabaseHandler() throws ClassNotFoundException {
        // Load the driver
        Class.forName("oracle.jdbc.driver.OracleDriver");
        isOpen = false;
    }
        
    /** 
     *  Sets the database host name, user login, and password.
     *  @param dbUser the username to log in with
     *  @param dbPass the password for the given username
     *  @param dbURL the host name/URL of the database
     */
    public synchronized void setConnectionDetails(String dbUser, String dbPass, String dbURL) {
        url = dbURL;
        user = dbUser;
        password = dbPass;        
    }
    
    /** 
     *  Attemps a connection, throws an exception if not possible.
     */
    public synchronized void attemptConnection() throws SQLException {
        createConnection();
        closeConnection(false);
    }
    
    /** 
     *  Returns TRUE if the connection is open.
     *  @return boolean
     */
    public boolean isOpen() { return isOpen; }
    
    /** 
     *  Allows a user to INSERT a new guest record into the database.
     *  @param info the field values for the new record
     *  @throws SQLException if the INSERT fails to execute
      */
    public void addRecord(GuestInfo info) throws SQLException {
        // Create the INSERT command
        String insert;
        insert  = "INSERT INTO Guest "
                + "VALUES ('" + info.getEmail() + "','"
                + info.getName() + "','" + info.getAddress() + "','"
                + info.getCity() + "','" + info.getPostcode() + "','"
                + info.getTelephone() + "','" + info.getPassword() 
                + "')";
        
        // Run the command
        executeCommand(insert);
    }
    
    /** 
     *  Allows a user to retrieve a record from the database.
     *  @param target indicates the record to retrieve
     *  @return GuestInfo, the retrieved record (null if no record was found)
     *  @throws SQLException if the UPDATE fails to execute
     */
    public synchronized GuestInfo retrieveRecord(GuestInfo target) throws SQLException {
        GuestInfo guest = null;
        ResultSet rs;
        String query;
        
        // Create the query
        query = "SELECT * FROM Guest WHERE Email = '" + target.getEmail() + "' ";
        rs = executeQuery(query);
        
        // Get the guest
        if (rs.next()) {
            guest = new GuestInfo( rs.getString("NAME"),
                                   rs.getString("ADDRESS"),
                                   rs.getString("CITY"),
                                   rs.getString("POSTCODE"),
                                   rs.getString("TELEPHONE"),
                                   rs.getString("EMAIL"),
                                   rs.getString("PASSWORD") );
        }
        
        closeConnection(true);        
        return guest;
    }
    
    /** 
     *  Allows a user to UPDATE a guest record in the database.
     *  @param target indicates the record to update, and the information to update with
     *  @throws SQLException if the UPDATE fails to execute
     */
    public void updateRecord(GuestInfo target) throws SQLException {
        // Create the UPDATE command
        String update;
        String set = "";
        if ( target.getName().equals("") == false ) set += " Name = '" + target.getName() + "', ";
        if ( target.getAddress().equals("") == false ) set += " Address = '" + target.getAddress() + "', ";
        if ( target.getCity().equals("") == false ) set += " City = '" + target.getCity() + "', ";
        if ( target.getPostcode().equals("") == false ) set += " Postcode = '" + target.getPostcode() + "', ";
        if ( target.getTelephone().equals("") == false ) set += " Telephone = '" + target.getTelephone() + "', ";
        if ( target.getPassword().equals("") == false ) set += " Password = '" + target.getPassword() + "', ";
        
        set = set.substring(0, set.length()-2); // Remove last comma
        set = " SET " + set;
        
        update  = "UPDATE Guest " + set  + " WHERE Email = '" + target.getEmail() + "' ";
        
        // Run the command
        executeCommand(update);
    }
    
    /** 
     *  Allows a user to DELETE a guest record from the database.
     *  @param target indicates the record to delete
     *  @throws SQLException if the DELETE fails to execute
     */
    public void deleteRecord(GuestInfo target) throws SQLException {
        // Create the DELETE command
        String delete = "DELETE FROM Guest ";
        delete += "WHERE email = '" + target.getEmail() + "'";
        
        // Run the command
        executeCommand(delete);
    }

    /** 
     *  Executes an update (INSERT, UPDATE, DELETE) or a query (SELECT) on the database.
     *  @param sql the SQL statement to execute.
     *  @throws ClassNotFoundException if the OracleDriver is not found
     */
    private synchronized void executeCommand(String sql) throws SQLException {
        boolean error = false;
        String msg = "";
        
        // Attempt to create the connection
        createConnection();
        createStatement();
        
        // Run the SQL statement
        try { 
            statement.executeUpdate( sql );
        }
        catch (SQLException sqlEx) {
            error = true;
            msg = "\nExecution Error:\n" + sqlEx;
        }

        // Attempt to close the connection       
        try {
            closeConnection(true);   
        }
        catch (SQLException sqlEx) {
            error = true;
            msg = "\nDisconnect Error:\n" + sqlEx;            
        }
        
        // Throw exception if necessary
        if ( error ) throw new SQLException( msg );
    }
    
    /** 
     *  Executes a query
     *  @return ResultSet
     *  @throws SQLException if there is a problem accessing the database
     */
    private synchronized ResultSet executeQuery(String query) throws SQLException {
        createConnection();
        createStatement();        
        return statement.executeQuery(query);
    }
    
    /** 
     *  This method uses a GuestInfo object to contain the criteria to query
     * the database on. 
     *  One problem with this is that the 'email' field of a GuestInfo object cannot be null.
     * Because of this, the email field of the GuestInfo should be a valid dummy value, e.g.
     * 'x@x.x'.
     *  Fields in the GuestInfo which are null/empty will not be included in the query.
     *  
     * @param criteria a GuestInfo object with its fields set to the search criteria
     * @return ArrayList<GuestInfo> of the matching records
     * @throws SQLException if there is a problem accessing the database
     */
    public synchronized ArrayList<GuestInfo> retrieveUsers(GuestInfo criteria) throws SQLException {
        String selection = "";
        String condition = "";
        
        // Get the GuestInfo fields being used as criteria
        HashMap<String, String> fields = new HashMap<String, String>(6);
        if (criteria.getEmail().equals(DUMMY_EMAIL) == false) fields.put("EMAIL", criteria.getEmail());
        if (criteria.getName().equals("") == false) fields.put("NAME", criteria.getName());
        if (criteria.getAddress().equals("") == false) fields.put("ADDRESS", criteria.getAddress());
        if (criteria.getCity().equals("") == false) fields.put("CITY", criteria.getCity());
        if (criteria.getPostcode().equals("") == false) fields.put("POSTCODE", criteria.getPostcode());
        if (criteria.getTelephone().equals("") == false) fields.put("TELEPHONE", criteria.getTelephone());
        if (criteria.getPassword().equals("") == false) fields.put("PASSWORD", criteria.getPassword());
        
        // Construct the SELECT fields and the WHERE conditions
        if (fields.isEmpty() == false) {
            HashSet<String> fieldNames = new HashSet<String>( fields.keySet() );
            String key;
            Iterator<String> iter = fieldNames.iterator();
            condition += " WHERE ";
            
            while (iter.hasNext()) {
                key = iter.next();
                condition += key + " = '" + fields.get( key ) + "' ";            
                if (iter.hasNext()) condition += " AND ";
            }
        }

        // Construct and execute the query
        String query = "SELECT * FROM Guest" + condition + " ORDER BY Name ";
        ResultSet rs = executeQuery( query );
        
        // Get the records from the ResultSet
        ArrayList<GuestInfo> users = new ArrayList<GuestInfo>();
        GuestInfo nextGuest;
        while (rs.next()) {
            nextGuest = new GuestInfo( rs.getString("NAME"),
                                   rs.getString("ADDRESS"),
                                   rs.getString("CITY"),
                                   rs.getString("POSTCODE"),
                                   rs.getString("TELEPHONE"),
                                   rs.getString("EMAIL"),
                                   rs.getString("PASSWORD") );
            users.add(nextGuest);
        }
        
        closeConnection(false);
        return users;
    }
    
    
    /** 
     *  Checks if an admin exists
     *  @param email the email to query for
     *  @return boolean indicating if user is an admin
     *  @throws SQLException if there is a problem accessing the database
     */    
     public boolean adminExists(String email) throws SQLException {
        return recordExists("GuestAdmin", "EMAIL", email);
    }
    
    /** 
     *  Checks if a user exists
     *  @param email the email to query for
     *  @throws SQLException if there is a problem accessing the database
     */
    public boolean userExists(String email) throws SQLException {
        return recordExists("Guest", "EMAIL", email);
    }
    
    /** 
     *  Verifies that a login is valid
     *  @param email the email to check for
     *  @param pass the password for the email
     *  @return boolean indicating if email and pass are valid
     *  @throws SQLException if there is a problem accessing the database
     */
    public synchronized boolean validLogin(String email, String pass) throws SQLException {
        boolean exists = false;
        String query = "SELECT * FROM Guest WHERE Email = '" + email + "' AND Password = '" + pass + "'";
        
        ResultSet rs = executeQuery(query);
        if (rs.next()) exists = true;
        
        closeConnection(false);
        return exists;
    }
    
    /** 
     *  Checks if a record with the given value exists in a table
     *  @param table the table to query
     *  @param attribute the column to check
     *  @param value the value of the attribute to search for
     *  @return boolean indicating if the record exists
     *  @throws SQLException if there is a problem accessing the database
     */
    private synchronized boolean recordExists(String table, String attribute, String value) throws SQLException {
        boolean exists = false;
        String query = "SELECT " + attribute + " FROM " + table + " WHERE " + attribute + " = '" + value + "' ";
        ResultSet rs = executeQuery(query);
        
        if (rs.next()) exists = true;
        
        closeConnection(false);        
        return exists;
    }

    /**  
     * Adds a server log to the database
     * @param log the GuestLog to add
     * @throws SQLException if there is a problem accessing the database
     */
    public void addGuestLog(GuestLog log) throws SQLException {
        // Create the INSERT command
        String dateFormat = "YYYY-MM-DD HH24:MI:SS";
        String insert = "INSERT INTO GuestLog VALUES( "
                      + log.getGLID() + ","
                      + "'" + log.getEmail() + "',"
                      + "'" + log.getIP() + "',"
                      + "to_date('" + log.getDate() + "','" + dateFormat + "') "
                      + ") ";
        executeCommand(insert);        
    }
    
    /**  
     * Adds and entry to the guest book
     * @param entry the GuestEntry to add
     * @throws SQLException if there is a problem accessing the database
     */
    public void addGuestEntry(GuestEntry entry) throws SQLException {
        // Create the INSERT command
        String dateFormat = "YYYY-MM-DD HH24:MI:SS";
        String insert = "INSERT INTO GuestEntry VALUES( "
                      + entry.getGEID() + ","
                      + "'" + entry.getEmail() + "',"
                      + "'" + entry.getEntry() + "',"
                      + "to_date('" + entry.getDate() + "','" + dateFormat + "') "
                      + ") ";
        executeCommand(insert);        
    }
    
    /** 
     *  Retrieves all GuestLog records from the database.
     *  @return ArrayList<GuestLog> of the records
     *  @throws SQLException if there is a problem accessing the database
     */
    public synchronized ArrayList<GuestLog> getGuestLogs() throws SQLException {
        ArrayList<GuestLog> logs = new ArrayList<GuestLog>();
        GuestLog nextLog;
        
        ResultSet rs = executeQuery("SELECT GLID, EMAIL, IPADDRESS, TO_CHAR(LOGDATE, 'YYYY-MM-DD HH24:MI:SS') AS LDATE FROM GUESTLOG ORDER BY LDATE DESC ");
        while(rs.next()) {
            nextLog = new GuestLog( rs.getLong("GLID"), rs.getString("EMAIL"),
                                    rs.getString("IPADDRESS"), rs.getString("LDATE") );
            logs.add( nextLog );
        }
        
        closeConnection(false);        
        return logs;
    }

    /**  
     *  Retrieves all GuestEntry records from the database.
     *  @return ArrayList<GuestEntry> of the records
     *  @throws SQLException if there is a problem accessing the database
     */
    public synchronized ArrayList<GuestEntry> getGuestEntries() throws SQLException {
        ArrayList<GuestEntry> entries = new ArrayList<GuestEntry>();
        GuestEntry nextEntry;
        
        ResultSet rs = executeQuery("SELECT GEID, EMAIL, ENTRY, TO_CHAR(ENTRYDATE, 'YYYY-MM-DD HH24:MI:SS') AS EDATE FROM GUESTENTRY ORDER BY EDATE DESC ");
        while(rs.next()) {
            nextEntry = new GuestEntry( rs.getLong("GEID"), rs.getString("EMAIL"),
                                        rs.getString("ENTRY"), rs.getString("EDATE") );
            entries.add( nextEntry );
        }
        
        closeConnection(false);        
        return entries;
    }    
    
    /** 
     *  Retrieves the maximum GEID in the database
     *  @return long indicating the maximum GEID
     *  @throws SQLException if there is a problem accessing the database
     */
    public long getMaxGEID() throws SQLException {
        return getMaxID("GEID", "GuestEntry");
    }    
    
    /** 
     *  Retrieves the maximum GLID in the database
     *  @return long indicating the maximum GLID
     *  @throws SQLException if there is a problem accessing the database
     */
    public long getMaxGLID() throws SQLException {
        return getMaxID("GLID", "GuestLog");
    }
    
    /** 
     *  Returns the highest ID from a table.
     *  @param idName the name of the column to get the id from
     *  @param table the table to query
     *  @return long indicating the max ID in a relation
     *  @throws SQLException if there is a problem accessing the database
     */
    private synchronized long getMaxID(String idName, String table) throws SQLException {
        long max = -1;
        String query = "SELECT MAX(" + idName + ") FROM " + table + " ";
        ResultSet rs = executeQuery(query);
        
        if (rs.next()) max = rs.getLong(1);
        
        closeConnection(false);        
        return max;
    }
    
    /** 
     *  Attempts to create a connection to the indicated Oracle database.
     *  @throws SQLException if there is a problem accessing the database
     */
    private synchronized void createConnection() throws SQLException {        
        // Create the connection
        try { guestConn = DriverManager.getConnection(url, user, password); }
        catch (SQLException sqlEx) {
            throw new SQLException( sqlEx.getMessage() + "\nSPECIFIC: Connection to database failed.");
        }
        
        try { guestConn.setAutoCommit(false); }
        catch (SQLException sqlEx) {
            closeConnection(false);
            throw new SQLException( sqlEx.getMessage() + "\nSPECIFIC: AutoCommit configuration failed.");    
        }
        
        try { guestConn.setReadOnly(false); }
        catch (SQLException sqlEx) {
            closeConnection(false);
            throw new SQLException( sqlEx.getMessage() + "\nSPECIFIC: ReadOnly configuration failed.");
        }
        
        // Only executed if the connection is successful
        isOpen = true;  
    }
    
    /** 
     *  Attempts to close the connection to the database
     *  @param doCommit indicates whether or not a COMMIT should be performed on closing.
     *  @throws SQLException if the database cannot be accessed
     */
    private synchronized void closeConnection(boolean doCommit) throws SQLException {
        if (isOpen() == false) return;
        
        if (doCommit) guestConn.commit();
        else guestConn.rollback();
        
        try { guestConn.close(); }
        catch (SQLException sqlEx) {
            throw new SQLException(sqlEx.getMessage() + "\nSPECIFIC: Closing connection failed.");
        }
        
        finally {
            isOpen = false;
        }
    }
    
    /** 
     *  Attempts to create a statement with the open connection.
     *  @throws SQLException if the Statement cannot be created.
     */
    private synchronized void createStatement() throws SQLException {
        if ( isOpen ) {
            statement = guestConn.createStatement();
        }
    }      
}
