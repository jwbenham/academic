
package guestbook.server;

import guestbook.shared.*;

import javax.swing.JComponent;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JPanel;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.sql.SQLException;
import java.io.IOException;

/**
 *  The ServerGUI provides a graphical user interface for
 *  interacting with a GuestBookServer.
 *              
 *  @author Joram Benham 200801274
 *  @version April 8, 2011
 */
public class ServerGUI extends JFrame implements ActionListener {

    private GuestBookServer server; // The server thread

    private JLabel statusLabel; // Displays server status
    
    private JTextField waitField;   // Text field for how long the server waits for connection interval
    private JTextField portField;   // Text field for server port number
    private JTextField handlersField;   // Text field for number of client handlers
    private JTextField urlField;   // Text field for the database URL
    private JTextField usernameField;    // Text field for the username to access the database
    private JPasswordField passField;    // Password field to enter the database password
    
    private JButton startButton;    // Click to start the server
    private JButton stopButton;     // Click to stop the server

    /** 
     *  Initializes the ServerGUI
     */
    public ServerGUI() {
        // Initialize the frame
        super("GuestBookServer GUI");
        setLayout( new FlowLayout() );
        setSize( GUIUtils.MIN_SIZE );
        setMinimumSize( GUIUtils.MIN_SIZE );
        getContentPane().setBackground( GUIUtils.BG_COLOR );
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
                      
        // Label initialization
        statusLabel = GUIUtils.createStyledLabel("Stopped", GUIUtils.TEXT_FONT, Color.CYAN);
        
        // Text field initialization
        int fieldSize = 25;
        portField = new JTextField("9000", fieldSize);
        handlersField = new JTextField("5", fieldSize);
        waitField = new JTextField("100", fieldSize);
        urlField = new JTextField("jdbc:oracle:thin:@xisbobcat.stfx.ca:1521:CSCI275", fieldSize);
        usernameField = new JTextField("user", fieldSize);
        passField = new JPasswordField(fieldSize);
        
        // Button initialization
        startButton = new JButton("Start");
        startButton.addActionListener( this );
        stopButton = new JButton("Stop");
        stopButton.addActionListener( this );
        
        // Title panel initialization
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground( GUIUtils.BG_COLOR );
        titlePanel.add( GUIUtils.createHeader("GuestBookServer Controller") );
        
        // Control panel initialization
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout( new BoxLayout(controlPanel, BoxLayout.Y_AXIS) );
        controlPanel.setBackground( GUIUtils.BG_COLOR );
        
        Dimension itemSizeMax = new Dimension(200, 20);
        Dimension gap = new Dimension(10, 5);
        GUIUtils.addBoxRow( controlPanel, 
                            new JComponent[]{GUIUtils.createTitle("Port #"), portField}, 
                            itemSizeMax, gap );
        GUIUtils.addBoxRow( controlPanel, 
                            new JComponent[]{GUIUtils.createTitle("Client Handlers"), handlersField},
                            itemSizeMax, gap );
        GUIUtils.addBoxRow( controlPanel, 
                            new JComponent[]{GUIUtils.createTitle("Timeout Interval(ms)"), waitField},
                            itemSizeMax, gap );
        GUIUtils.addBoxRow( controlPanel, 
                            new JComponent[]{GUIUtils.createTitle("Database URL"), urlField},
                            itemSizeMax, gap );
        GUIUtils.addBoxRow( controlPanel, 
                            new JComponent[]{GUIUtils.createTitle("Database Username"), usernameField},
                            itemSizeMax, gap );
        GUIUtils.addBoxRow( controlPanel, 
                            new JComponent[]{GUIUtils.createTitle("Database Password"), passField},
                            itemSizeMax, new Dimension(10,35) );
        GUIUtils.addBoxRow( controlPanel,
                            new JComponent[]{startButton, stopButton},
                            itemSizeMax, gap );
        
        // Status panel initialization
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout( new BoxLayout(statusPanel, BoxLayout.Y_AXIS) );
        statusPanel.setBackground( GUIUtils.BG_COLOR );
        
        JLabel statusTitle = GUIUtils.createTitle("Status: "); 
        statusTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        GUIUtils.addBoxRow( statusPanel,
                            new JComponent[]{ statusTitle, statusLabel },
                            new Dimension(50, 20), new Dimension(10,10) );
                            
        // Display
        Box contentBox = Box.createVerticalBox();
        contentBox.add( titlePanel );
        contentBox.add( Box.createVerticalStrut(20) );
        contentBox.add( controlPanel );
        contentBox.add( Box.createVerticalStrut(20) );
        contentBox.add( statusPanel );
        
        add( contentBox );
        setVisible(true);
        
        // Create server
        try {
            server = new GuestBookServer(statusLabel);
        }
        catch (ClassNotFoundException classEx) {
            GUIUtils.showErrorMessage(null, "Database Error", "The JDBC-Oracle Driver could not be found.", classEx);
        }
    }

    
    /** 
     *  Implements the actions taken when this listener receives an
     *  ActionEvent.
     *  @param ev the received ActionEvent
     */
    public void actionPerformed(ActionEvent ev) {
        Object source = ev.getSource();
        
        // Case: source == startButton
        if (source == startButton) {
            try {
                server = new GuestBookServer(statusLabel);
                
                // Try to connect to the database.
                String url = urlField.getText();
                String user = usernameField.getText();
                String pass = String.valueOf( passField.getPassword() );            
                server.initDatabase(user, pass, url);
                
                // Try to initialize the server
                int thePort = Integer.parseInt( portField.getText() );
                int handlers = Integer.parseInt( handlersField.getText() );
                int wait = Integer.parseInt( waitField.getText() );
                
                if (server.init(thePort, handlers, wait)) {
                    server.startListening();   
                }

                toggleInteractiveComponents( false );            
            }
            catch (ClassNotFoundException classEx) {
                GUIUtils.showErrorMessage(null, "Driver Error", "A problem occurred with the Oracle driver.", classEx);
            }
            catch (SQLException sqlEx) {
                GUIUtils.showErrorMessage(null, "SQL Error", "A problem occurred while accessing the database.", sqlEx);
            }
            catch (IOException ioEx) {
                GUIUtils.showErrorMessage(null, "Server Error", "The server could not be established.", ioEx);
            }
            catch (NumberFormatException formatEx) {
                GUIUtils.showErrorMessage(null, "Format Error", "Enter integers in the port, handlers, and timeout fields.", formatEx);    
            }
            catch (IllegalArgumentException argEx) {
                GUIUtils.showErrorMessage(null, "Input Error", "An input value was illegal.", argEx);
            }
        }
        
        // Case: source == stopButton 
        else if (source == stopButton) {
            server.terminate();                        
            toggleInteractiveComponents( true );            
        }
    }
    
    /** 
     *  Toggles components to inactive/active states.
     *  @param toggle: boolean used to set active/inactive states
     */
    private void toggleInteractiveComponents(boolean toggle) {
            portField.setEditable( toggle );
            handlersField.setEditable( toggle );
            waitField.setEditable( toggle );
            urlField.setEditable( toggle );
            usernameField.setEditable( toggle );
            passField.setEditable( toggle );
            startButton.setEnabled( toggle );
            
            stopButton.setEnabled( !toggle );        
    }

//*****************************************************************************************************
// main()
//*****************************************************************************************************

    public static void main(String[] args) {
        ServerGUI sg = new ServerGUI();
    }

}
