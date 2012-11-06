package guestbook.client;

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
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JScrollPane;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Font;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.sql.SQLException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Vector;

/** 
 *  The ClientGUI displays an AdminPanel if the logged in user
 *  is an administrator. The AdminPanel allows the administrator
 *  to display users in the database, the server logs, and to delete
 *  a user.
 *              
 *  @author Joram Benham 200801274
 *  @version April 8, 2011
 */
public class AdminPanel extends JPanel implements ActionListener {

    private GuestBookClient client; // Reference to client
    private String userEmail;       // email of current user

    private JScrollPane viewPane;   // Holds the displayed table
    private JTable viewTable;       // Displays server logs and guests
    
    private GuestInfoFieldBox guestInfoBox;
    
    private JButton viewUsersButton;    // click to view users
    private JButton deleteUserButton;   // click to delete a user
    private JButton viewLogsButton;     // click to view server logs
    
    private JTextField deleteField;
    private JTextField userLogField;
    private JTextField dateLogField;
    
    /** Constructor
     * @param theClient reference to the client
     * @param theUser email of current user
     */
    public AdminPanel(GuestBookClient theClient, String theUser) {
        
        // Initialize the panel
        if (theClient == null || theUser == null) {
			throw new NullPointerException("Constructor arguments cannot be null.");
		}
        client = theClient;
        userEmail = theUser;
        setLayout( new BoxLayout(this, BoxLayout.Y_AXIS) );
        setBackground( GUIUtils.BG_COLOR ); 
        
        // Initialize the buttons
        viewUsersButton = new JButton("View Users");
        viewUsersButton.addActionListener(this);
        deleteUserButton = new JButton("Delete User");
        deleteUserButton.addActionListener(this);
        viewLogsButton = new JButton("View Logs");
        viewLogsButton.addActionListener(this);
        
        // Initialize the text fields
        int size = 15;
        deleteField = new JTextField(size);
        userLogField = new JTextField(size);
        dateLogField = new JTextField(size);
        
        // Create the viewBox
        Box viewBox = Box.createHorizontalBox();
        viewBox.setMinimumSize( new Dimension(1000, 300) );
        viewBox.setPreferredSize( new Dimension(1000, 300) );
        
        viewPane = new JScrollPane();
        viewPane.setMinimumSize( new Dimension(1000, 300) );
        viewPane.setPreferredSize( new Dimension(1000, 300) );
        viewBox.add( viewPane );              
        
        // Create the controlBox
        JPanel controlBox = new JPanel();
        controlBox.setLayout( new GridLayout(1, 3) );
        controlBox.setMinimumSize( new Dimension(1000, 300) );
        controlBox.setMaximumSize( new Dimension(1000, 300) );

        // Create the "view users" column
        JPanel viewUsersColumn = new JPanel();
        viewUsersColumn.setBackground( GUIUtils.BG_COLOR );
        viewUsersColumn.setLayout( new BorderLayout() );
        guestInfoBox = new GuestInfoFieldBox(userEmail, true);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add( viewUsersButton );
        buttonPanel.setBackground( GUIUtils.BG_COLOR );
        
        viewUsersColumn.add( GUIUtils.createSubHeader("View Matching Users"), 
							BorderLayout.NORTH );
        viewUsersColumn.add( guestInfoBox, BorderLayout.CENTER );
        viewUsersColumn.add( buttonPanel, BorderLayout.SOUTH );
        
        // Create the "delete user" column
        JPanel deleteUserColumn = new JPanel();
        deleteUserColumn.setBackground( GUIUtils.BG_COLOR );
        deleteUserColumn.setLayout( new BorderLayout() );
        
        buttonPanel = new JPanel();
        buttonPanel.add( deleteUserButton );
        buttonPanel.setBackground( GUIUtils.BG_COLOR );
        
        Box deleteBox = Box.createVerticalBox();
        GUIUtils.addBoxRow( deleteBox,
                            new JComponent[]{ GUIUtils.createTitle("Target Email"),
						   						deleteField 
											},
                            new Dimension(100, 20), new Dimension(5, 40) 
						);
                            
        JPanel deletePanel = new JPanel();
        deletePanel.setBackground( GUIUtils.BG_COLOR );
        deletePanel.add( deleteBox );
        deleteUserColumn.add( GUIUtils.createSubHeader("Delete User"), 
								BorderLayout.NORTH );
        deleteUserColumn.add( deletePanel, BorderLayout.CENTER );
        deleteUserColumn.add( buttonPanel, BorderLayout.SOUTH );
        
        // Create the "view logs" column
        JPanel viewLogsColumn = new JPanel();
        viewLogsColumn.setBackground( GUIUtils.BG_COLOR );
        viewLogsColumn.setLayout( new BorderLayout() );
        
        buttonPanel = new JPanel();
        buttonPanel.add( viewLogsButton );
        buttonPanel.setBackground( GUIUtils.BG_COLOR );
        
        Box logBox = Box.createVerticalBox();
        GUIUtils.addBoxRow( logBox,
                            new JComponent[]{ GUIUtils.createTitle("By Email"), 
												userLogField },
                            new Dimension(100, 20), new Dimension(5, 10) );
        GUIUtils.addBoxRow( logBox,
                            new JComponent[]{ 
								GUIUtils.createTitle("By Date (yyyy-mm-dd)"), 
								dateLogField },
                            new Dimension(100, 20), new Dimension(5, 10) );
                            
        JPanel logPanel = new JPanel();
        logPanel.setBackground( GUIUtils.BG_COLOR );
        logPanel.add( logBox );
        
        viewLogsColumn.add( GUIUtils.createSubHeader("Log Viewer"), 
							BorderLayout.NORTH );
        viewLogsColumn.add( logPanel, BorderLayout.CENTER );
        viewLogsColumn.add( buttonPanel, BorderLayout.SOUTH );
        
        // Add the columns to the control box
        controlBox.add( viewUsersColumn );
        controlBox.add( deleteUserColumn );
        controlBox.add( viewLogsColumn );
        
        // Display
        add( viewBox );
        add( controlBox );
        setVisible( true );
    }
    
    /** 
     *  Determines which method to call when an ActionEvent is generated
     * @param e the ActionEvent generated
     */
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
    
        if (source == viewUsersButton) doViewUsers();
        else if (source == deleteUserButton) doDeleteUser();
        else if (source == viewLogsButton) doViewLogs();        
    }
    
    /** 
     *  Sends a message to the client to get a list of users from the server.
     */
    private void doViewUsers() {
        try {
            GuestInfo criteria = guestInfoBox.getGuestInfo();
            ArrayList<GuestInfo> guests = client.getUsers(criteria);
            
            if (guests == null) {
				GUIUtils.showErrorMessage(this, "No Results", 
						"No users matching the criteria were found.", null);
			} 
			else {
				showUsersInTable( guests );
			}
        }
        catch (IllegalArgumentException illegalArgEx) {
            GUIUtils.showErrorMessage(this, "Illegal Argument", 
					"One or more of the user details you entered were invalid.", 
					illegalArgEx);
        }
        catch (IOException ioEx) {
            GUIUtils.showErrorMessage(this, "Network Error", 
					"A problem occurred with the network connection.", ioEx);   
        }
        catch (Exception ex) {
            GUIUtils.showErrorMessage(this, "Unknown Error", 
					"A problem occurred during the retrieval attempt.", ex);    
        }
    }
    
    /** 
     *  Sends a message to the client to send a delete user request to the server.
     */
    private void doDeleteUser() {
        try {
            GuestInfo target = new GuestInfo( deleteField.getText() );
            
            if ( client.deleteRecord(target) ) {
                GUIUtils.showInfoMessage(this, "Delete Success", 
						"The user was successfully deleted.");  
            } else {
                GUIUtils.showErrorMessage(this, "Delete Failure", 
						"The operation failed. The address may not exist.", null); 
            }
        }
        catch (IllegalArgumentException illegalArgEx) {
            GUIUtils.showErrorMessage(this, "Illegal Argument", 
					"The email address you entered is invalid.", illegalArgEx);
        }
        catch (IOException ioEx) {
            GUIUtils.showErrorMessage(this, "Network Error", 
					"A problem occurred with the network connection.", ioEx);   
        }
        catch (Exception ex) {
            GUIUtils.showErrorMessage(this, "Unknown Error", 
					"A problem occurred during the deletion attempt.", ex);    
        }
    }
    
    /** 
     *  Sends a message to the client to send a "get logs" request to the server.
     */
    private void doViewLogs() {
        try {
            // Get the email to filter results, if given
            String inputEmail = userLogField.getText();
            GuestInfo emailCriteria;
            if (inputEmail.equals("") == false) {
			   	emailCriteria = new GuestInfo(inputEmail);
			}
            
            // Get the date to filter results, if given
            String date = dateLogField.getText();
            if (date.equals("") == false) {
                if ( date.matches("\\d\\d\\d\\d-\\d\\d-\\d\\d") == false ) {
				   	throw new IllegalArgumentException(
							"Date must have format 'yyyy-mm-dd'."
					);
				}
            }
            
            // Get the logs and display
            ArrayList<GuestLog> logs = client.getLogs();
            
            if (logs == null) {
				GUIUtils.showErrorMessage(this, "No Logs", "No logs were found.", 
											null);
			}
            else showLogsInTable( logs, inputEmail, date );
        }
        catch (IllegalArgumentException illegalArgEx) {
            GUIUtils.showErrorMessage(this, "Illegal Argument", 
					"One or more of the criteria you entered were invalid.", 
					illegalArgEx);
        }
        catch (IOException ioEx) {
            GUIUtils.showErrorMessage(this, "Network Error", 
					"A problem occurred with the network connection.", ioEx);   
        }
        catch (Exception ex) {
            GUIUtils.showErrorMessage(this, "Unknown Error", 
					"A problem occurred during the retrieval attempt.", ex);    
        }  
    }
    
    /** 
     *  Creates the Vectors of row data and column headers necessary to create a table showing
     *  the server logs.
     *  @param logs the server logs
     *  @param email the email to filter by
     *  @param date the date to filter by
     */
    private void showLogsInTable( 
			ArrayList<GuestLog> logs, 
			String email, 
			String date ) {

        if (logs.isEmpty()) return;
        
        boolean checkEmail = false; // Indicates whether to filter by email
        boolean checkDate = false;  // Indicates whether to filter by date
        
        if ( !email.equals("") ) checkEmail = true;
        if ( !date.equals("") ) checkDate = true;
        
        // Create the headers
        Vector<String> headers = new Vector<String>();
        headers.add("Log ID");
        headers.add("Email");
        headers.add("IP Address");
        headers.add("Date");
        
        // Create the row data based on the server logs
        Vector< Vector<String> > data = new Vector< Vector<String> >();
        Vector<String> nextRow = new Vector<String>();
        GuestLog nextLog;
        boolean addThisLog = true;
        
        for (int i = 0; i < logs.size(); i++) {
            addThisLog = true;
            nextLog = logs.get(i);            
            
            if ( checkEmail ) {
                if ( nextLog.getEmail().equals(email) ) addThisLog = true;
                else addThisLog = false;
            }
            
            if ( checkDate ) {
                if ( nextLog.getDateOnly().equals(date) ) addThisLog = true;
                else addThisLog = false;
            }
            
            if ( addThisLog ) {
                nextRow = new Vector<String>();
                nextRow.add( String.valueOf(nextLog.getGLID()) );
                nextRow.add( nextLog.getEmail() );
                nextRow.add( nextLog.getIP() );
                nextRow.add( nextLog.getDate() );
                
                data.add( nextRow );
            }
        }
        
        createTable( data, headers );
    }
    
    /** 
     *  Creates the Vectors of row data and column headers necessary to create 
	 *  a table showing the guest book users.
     *  @param users the users in the database (as GuestInfo objects in an 
	 * 				ArrayList)
     */
    private void showUsersInTable( ArrayList<GuestInfo> users ) {
        if (users.isEmpty()) return;
        
        // Create the headers
        Vector<String> headers = new Vector<String>();
        headers.add("Email");
        headers.add("Password");
        headers.add("Name");
        headers.add("Address");
        headers.add("City");
        headers.add("Postcode");
        headers.add("Telephone");
        
        // Create the row data based on the users
        Vector< Vector<String> > data = new Vector< Vector<String> >();
        Vector<String> nextRow = new Vector<String>();
        GuestInfo nextUser;
        
        for (int i = 0; i < users.size(); i++) {
            nextUser = users.get(i);
            
            nextRow = new Vector<String>();
            nextRow.add(nextUser.getEmail());
            nextRow.add(nextUser.getPassword());
            nextRow.add(nextUser.getName());
            nextRow.add(nextUser.getAddress());
            nextRow.add(nextUser.getCity());
            nextRow.add(nextUser.getPostcode());
            nextRow.add(nextUser.getTelephone());
            
            data.add( nextRow );
        }
        
        createTable( data, headers );
    }
    
    /**
     *  Creates the table using the given row data and column headers
     *  @param rowData used to create the rows
     *  @param columnNames titles for each column
     */
    private void createTable(
			Vector< Vector<String> > rowData, 
			Vector<String> columnNames) {

        viewTable = new JTable(rowData, columnNames);
        viewTable.setRowHeight( 30 );
        viewTable.setRowSelectionAllowed( false );
        viewTable.setColumnSelectionAllowed( false );
        viewTable.setDragEnabled( false );
        
        viewPane.setViewportView( viewTable );       
    }

}
