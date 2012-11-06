
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
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;

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
import java.util.GregorianCalendar;

/** 
 *  The ClientGUI displays a GuestPanel if the logged in user
 *  is an ordinary guest user or admin. The GuestPanel allows the
 *  user to view and update their record in the database, and to add and
 *  view entries to the guest book.
 *              
 *  @author Joram Benham 200801274
 *  @version April 8, 2011
 */
public class GuestPanel extends JPanel implements ActionListener {
    
    private GuestBookClient client; // Reference to the client
    private String userEmail;       // current user's email
    
    private GuestInfoFieldBox userDataFields;   // shows the user their record
    private GuestInfoFieldBox updateDataFields; // lets the user update their record
    
    private JButton refreshUserButton;      // click to reload user info
    private JButton submitDetailsButton;    // click to submit new info
    private JButton refreshBookButton;      // click to reload guest book
    private JButton submitEntryButton;      // click to submit new guest book entry
    
    private JTextArea newEntryArea;     // holds space to enter new entry
    private JTextArea bookEntryArea;    // holds the previous guest book entries
    
    /**Constructor
     * @param theClient reference to the client
     * @param theUser current user's email
     */
    public GuestPanel(GuestBookClient theClient, String theUser) {
        
        // Initialize the panel
        if (theClient == null || theUser == null) throw new NullPointerException("Constructor arguments cannot be null.");
        client = theClient;
        userEmail = theUser;
        setLayout( new GridLayout(2, 2) );
        setMinimumSize(GUIUtils.MIN_SIZE);
        setBackground(GUIUtils.BG_COLOR);
        
        // Initialize the text areas
        newEntryArea = new JTextArea(5, 10);
        newEntryArea.setLineWrap( true );
        newEntryArea.setWrapStyleWord( true );
        newEntryArea.setBackground( Color.WHITE );
        
        bookEntryArea = new JTextArea(5, 10);
        bookEntryArea.setLineWrap( true );
        bookEntryArea.setWrapStyleWord( true );
        bookEntryArea.setBackground( Color.LIGHT_GRAY );
        bookEntryArea.setEditable( false );
        
        // Initialize the buttons
        refreshUserButton = new JButton("Refresh User");
        refreshUserButton.addActionListener(this);
        submitDetailsButton = new JButton("Submit Details");
        submitDetailsButton.addActionListener(this);
        refreshBookButton = new JButton("Refresh Guest Book");
        refreshBookButton.addActionListener(this);
        submitEntryButton = new JButton("Submit Entry");
        submitEntryButton.addActionListener(this);
        
        // Initialize the "User Data" box
        JPanel userDataBox = new JPanel();
        userDataBox.setLayout( new BorderLayout() );
        userDataBox.setBorder( new LineBorder(Color.LIGHT_GRAY) );
        userDataBox.setBackground( GUIUtils.BG_COLOR );
        userDataFields = new GuestInfoFieldBox(userEmail, true);
        userDataFields.setEditable(false);
        
        userDataBox.add( GUIUtils.createSubHeader("Current Guest Details"), BorderLayout.NORTH );
        userDataBox.add( userDataFields, BorderLayout.CENTER );        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground( GUIUtils.BG_COLOR );
        buttonPanel.add( refreshUserButton );
        userDataBox.add( buttonPanel, BorderLayout.SOUTH );
        
        // Initialize the guestBookBox
        JPanel guestBookBox = new JPanel();
        guestBookBox.setLayout( new BorderLayout() );
        guestBookBox.setBorder( new LineBorder(Color.LIGHT_GRAY) );
        guestBookBox.setBackground( GUIUtils.BG_COLOR );
        
        guestBookBox.add( GUIUtils.createSubHeader("View Guest Book"), BorderLayout.NORTH );
        
        JScrollPane bookEntryPane = new JScrollPane(bookEntryArea);
        guestBookBox.add( bookEntryPane, BorderLayout.CENTER );
        
        buttonPanel = new JPanel();
        buttonPanel.setBackground( GUIUtils.BG_COLOR );
        buttonPanel.add( refreshBookButton );
        guestBookBox.add( buttonPanel, BorderLayout.SOUTH );
        
        // Initialize the updateFieldsBox
        JPanel updateFieldsBox = new JPanel();
        updateFieldsBox.setLayout( new BorderLayout() );
        updateFieldsBox.setBorder( new LineBorder(Color.LIGHT_GRAY) );
        updateFieldsBox.setBackground( GUIUtils.BG_COLOR );
        updateDataFields = new GuestInfoFieldBox(userEmail, false);
        
        updateFieldsBox.add( GUIUtils.createSubHeader("Update Guest Details"), BorderLayout.NORTH );
        updateFieldsBox.add( updateDataFields, BorderLayout.CENTER );        
        buttonPanel = new JPanel();
        buttonPanel.setBackground( GUIUtils.BG_COLOR );
        buttonPanel.add( submitDetailsButton );
        updateFieldsBox.add( buttonPanel, BorderLayout.SOUTH );       
        
        // Initialize the writeEntryBox
        JPanel writeEntryBox = new JPanel();
        writeEntryBox.setLayout( new BorderLayout() );
        writeEntryBox.setBorder( new LineBorder(Color.LIGHT_GRAY) );
        writeEntryBox.setBackground( GUIUtils.BG_COLOR ); 

        writeEntryBox.add( GUIUtils.createSubHeader("Submit Entry"), BorderLayout.NORTH ); 
        
        JScrollPane writeEntryPane = new JScrollPane(newEntryArea);
        writeEntryBox.add( writeEntryPane, BorderLayout.CENTER );
        
        buttonPanel = new JPanel();
        buttonPanel.setBackground( GUIUtils.BG_COLOR );
        buttonPanel.add( submitEntryButton );
        writeEntryBox.add( buttonPanel, BorderLayout.SOUTH );
        
        // Display       
        add(userDataBox);
        add(guestBookBox);
        add(updateFieldsBox);
        add(writeEntryBox);
        setVisible(true);
    }
    
    /** 
     * @param e the ActionEvent generated
     */
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source == refreshUserButton) doRefreshUser();
        else if (source == submitDetailsButton) doSubmitDetails();
        else if (source == refreshBookButton) doRefreshBook();
        else if (source == submitEntryButton) doSubmitEntry();
    }
    
    /** 
     *  Uses the client to send a request for record retrieval to the server.
     */
    private void doRefreshUser() {
        try {
            GuestInfo retrieved = client.retrieveRecord(new GuestInfo(userEmail));            
            userDataFields.setGuestInfo( retrieved );
        }
        catch (IOException ioEx) {
            GUIUtils.showErrorMessage(this, "Network Error", "A problem occurred with the network connection.", ioEx);   
        }
        catch (Exception ex) {
            GUIUtils.showErrorMessage(this, "Unknown Error", "A problem occurred during the retrieval.", ex);    
        }
    }
    
    /** 
     *  Uses the client to send a request to update a record to the server.
     */
    private void doSubmitDetails() {
        try {
            GuestInfo submission = updateDataFields.getGuestInfo(); 
            
            if ( client.updateRecord(submission) ) {
                GUIUtils.showInfoMessage(this, "Update Success", "Your details were successfully updated.");    
            } 
            
            else {
                GUIUtils.showErrorMessage(this, "Update Failure", "Your details were unable to be updated.", null);
            }
        }
        catch (IllegalArgumentException illegalArgEx) {
            GUIUtils.showErrorMessage(this, "Illegal Guest Details", "One or more of the details to be submitted was invalid.", illegalArgEx);
        }
        catch (IOException ioEx) {
            GUIUtils.showErrorMessage(this, "Network Error", "A problem occurred with the network connection.", ioEx);  
        }
        catch (Exception ex) {
            GUIUtils.showErrorMessage(this, "Unknown Error", "A problem occurred during the update.", ex);  
        }
    }
    
    /** 
     *  Uses the client to send a request to retrieve the guest book entries from the server, ordered
     *  from most recent to oldest.
     */
    private void doRefreshBook() {
        try {
            ArrayList<GuestEntry> allEntries = client.getEntries();
            
            if (allEntries == null) GUIUtils.showErrorMessage(this, "No Entries", "No entries were found.", null);
            else showGuestBook( allEntries );
        }
        catch (IOException ioEx) {
            GUIUtils.showErrorMessage(this, "Network Error", "A problem occurred with the network connection.", ioEx);
        }
        catch (Exception ex) {
            GUIUtils.showErrorMessage(this, "Unknown Error", "A problem occurred during the retrieval.", ex);
        }
    }
    
    /** 
     *  Uses the client to send a request to submit a new guest book entry to the server.
     */
    private void doSubmitEntry() {
        try {
            String comment = newEntryArea.getText();
            if (comment.length() > 4000) throw new IllegalArgumentException("Comment must be less than 4000 characters.");
            
            GuestEntry newEntry = new GuestEntry(0, userEmail, comment, new GregorianCalendar());
            if ( client.submitComment(newEntry) ) {
                GUIUtils.showInfoMessage(this, "Submission Success", "Your comment was successfully submitted.");
            }
            else {
                GUIUtils.showErrorMessage(this, "Submission Failure", "Your comment failed to be submitted.", null);   
            }            
        }
        catch (IllegalArgumentException illegalArgEx) {
            GUIUtils.showErrorMessage(this, "Illegal Comment", "There is a problem with the comment you submitted.", illegalArgEx);
        }
        catch (IOException ioEx) {
            GUIUtils.showErrorMessage(this, "Network Error", "A problem occurred with the network connection.", ioEx);  
        }
        catch (Exception ex) {
            GUIUtils.showErrorMessage(this, "Unknown Error", "A problem occurred during the submission.", ex); 
        }
    }
    
    /** 
     *  Displays the guest book entries in the bookEntryArea
     *  @param entries the GuestEntry objects to display
     */
    private void showGuestBook( ArrayList<GuestEntry> entries ) {
        bookEntryArea.setText("");
        String newText = "";
        
        for (int i = 0; i < entries.size(); i++) {
            newText += entries.get(i).toString() + "\n\n";
        }
        
        bookEntryArea.setText(newText);
    }
   
}
