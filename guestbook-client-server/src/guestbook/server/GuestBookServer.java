
package guestbook.server;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Scanner;

import java.sql.SQLException;
import javax.swing.JLabel;


/**
 *      A GuestBookServer is a threaded application. When run it listens to the
 *  specified port for incoming service requests. When it receives a request
 *  it creates a new ClientHandler thread to handle the request, and continues
 *  listening at the port for further connection attempts.
 *      The GuestBookServer connects to an Oracle server via a RealDatabaseHandler.
 *  The server will fail to start if the server is not found, as it cannot service requests
 *  without the database.
 * 
 * @author Joram Benham, 200801274
 * @version April 8, 2011
 */
public class GuestBookServer extends Thread {  

    public static final int DEFAULT_PORT = 1012;
    public static final int DEFAULT_HANDLERS = 10;
    public static final int DEFAULT_TIMEOUT = 2000;

    private RealDatabaseHandler dbHandler;  // Handles database interaction
    
    private ServerSocket listener;          // Socket to listen on
    private ExecutorService handlerPool;    // Executes new ClientHandlers
    
    private int port;       // Port to listen on
    private int numHandlers;    // Max number of handlers to spawn
    private int timeout;    // Length of time to wait for a request before checking the serverEnabled member
    private boolean terminated;     // Indicates if the server is terminated
    private boolean serverEnabled;  // Indicates if the server should stop listening and try to terminate
    private boolean dbFound;    // Indicates if the database has been found
    
    private JLabel statusLabel;

    /** 
     *  Initializes the server, setting the port, number of handlers, and listen timeout
     *  to their defaults. The driver for the database is also loaded.
     *  @param update optional JLabel (can be null) for status updates
     *  @throws ClassNotFoundException if the JDBC-Oracle driver is not found
     */
    public GuestBookServer(JLabel update) throws ClassNotFoundException {
        if (update != null) statusLabel = update; 
        dbHandler = new RealDatabaseHandler();  // Attempt to load database driver
        
        // Set up server variables
        port = DEFAULT_PORT;
        numHandlers = DEFAULT_HANDLERS;
        timeout = DEFAULT_TIMEOUT;
        
        // Set up status variables
        terminated = true;
        serverEnabled = false; 
        dbFound = false;
        if (statusLabel != null) statusLabel.setText("Stopped");
    }
    
    /** 
     *  While the server is running, listens for connection attempts and creates
     *  handlers to deal with client connections.
     */
    public void run() {
        if (serverEnabled == false) return;
        if (dbFound == false) return;
        if (statusLabel != null) statusLabel.setText("Running");

        Socket client = null;       
      
        // Listen while the server is enabled
        while (serverEnabled) {            
            try {
                if (statusLabel != null) statusLabel.setText("Listening");
                client = listener.accept();
                
                if (statusLabel != null) statusLabel.setText("Connecting");
                handlerPool.execute( new ClientHandler(client, dbHandler) );
            }
            catch (SocketTimeoutException noConnectionEx) {
                // do nothing, just loop again and see if it should stop running
            }   
            catch (IOException ioEx) {
                System.err.println("Connection error: " + ioEx);
                ioEx.printStackTrace();
            }            
        }               
        
        try {
            listener.close();
        }        
        catch (IOException listenerCloseEx) {
            System.out.println(listenerCloseEx);
        }
        
        // Try to shutdown all ClientHandlers. Wait for 10 seconds after every attempt.
        handlerPool.shutdownNow();
        while (handlerPool.isTerminated() == false) {
            if (statusLabel != null) statusLabel.setText("Terminating connections");
            
            try { 
                handlerPool.awaitTermination( 10, TimeUnit.SECONDS ); 
            }
            catch(InterruptedException intEx) {}
            
            handlerPool.shutdownNow();
        }
        
        // Update to terminated status
        terminated = true;
        if (statusLabel != null) statusLabel.setText("Stopped");
    }     
    
    /** 
     *  Initializes the database, and attempts a connection, setting dbFound to true 
     *  if the connection is successful.
     *  @param dbUser the username to log in with
     *  @param dbPass the password for the given username
     *  @param dbURL the host name/URL of the database
     *  @throws SQLException if there is a database error
     */
    public void initDatabase(String dbUser, String dbPass, String dbURL) throws SQLException {
        dbHandler.setConnectionDetails(dbUser, dbPass, dbURL);
        dbHandler.attemptConnection();
        dbFound = true;
    }
    
    /**
     *  @return boolean true if the database is running
     */
    public boolean isRunning() { return !terminated; }
    
    /** 
     * @return boolean true if the database has been found successfully
     */
    public boolean dbFound() { return dbFound; }
    
    /** 
     *  Initializes the server; sets the port number to listen on, the number of client handlers to
     *  make available at maximum, and the timeout between listening attempts.
     *  @param thePort the port number
     *  @param handlers number of handlers
     *  @param time server timeout time
     *  @return boolean indicating intialization success
     *  @throws IllegalArgumentException if a parameter is invalid
     */
    public boolean init(int thePort, int handlers, int time) throws IllegalArgumentException {
        if (serverEnabled || !dbFound) return false;  // Can not initialize when server is running.        
        
        setPort( thePort );
        setNumHandlers( handlers );
        setTimeout( time );
        
        return true;    // Initialization was successful - no exceptions, and the server was not running
    }
    
    /** 
     *  Creates the handlerPool for client connections and starts the server listening on the
     *  specified port if it is not already running.
     *  @throws IOException if there is a network error
     */
    public synchronized void startListening() throws IOException {
        if ( serverEnabled == false && dbFound ) {  
            handlerPool = Executors.newFixedThreadPool(numHandlers);
            listener = new ServerSocket(port);    // Create the server socket
            listener.setSoTimeout( timeout );
            
            terminated = false;
            serverEnabled = true;
            start();
        }
    }    
    
    /** 
     *  Terminates the server by setting serverEnabled to false.
     */
    public synchronized void terminate() {
        serverEnabled = false;
    }      
    
    /** 
     *  Sets the port number to listen on.
     *  @param portNum the port to listen on
     *  @throws IllegalArgumentException if the portNum is < 0
     */
    public void setPort( int portNum ) throws IllegalArgumentException {
        if (portNum < 0) {
            throw new IllegalArgumentException("Value '" + portNum + "'is invalid. The port number must be > 0.");
        }
        port = portNum; // Otherwise set the port
    }
    
    /** 
     *  Sets the number of client handlers available in the handlerPool
     *  @param num the number of handlers
     *  @throws IllegalArgumentException if num < 1
     */
    public void setNumHandlers(int num) throws IllegalArgumentException {
        if (num < 1) {
            throw new IllegalArgumentException("Value '" + num + "' is invalid. The number of client handlers must be > 1.");
        }
        numHandlers = num; // Otherwise set the number of handlers
    }
    
    /** 
     *  Sets the maximum amount of time the server will wait for a client connection before it
     *  stops and checks to see if it should terminate.
     *  @param time the server timeout time
     *  @throws IllegalArgumentException if time value is invalid
     */
    public void setTimeout(int time) throws IllegalArgumentException {
        if (time < 10 || time > 200) {
            throw new IllegalArgumentException("Value '" + time + "' is invalid. The minimum timeout is 100ms; the maximum is 10000ms");
        }
        timeout = time; // Otherwise set the timeout
    }
}
