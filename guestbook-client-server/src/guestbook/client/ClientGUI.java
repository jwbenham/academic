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
 *  The ClientGUI is the parent container for the rest of the GUI components.
 *  It initially displays the LoginPanel, the GuestPanel if login is successful,
 *  and the AdminPanel if the user is an administrator.
 *              
 *  @author Joram Benham 200801274
 *  @version April 8, 2011
 */
public class ClientGUI extends JFrame implements ActionListener {
    
    private GuestBookClient client; // Reference to client
    
    private LoginPanel loginPanel;
    private GuestPanel guestPanel;
    private AdminPanel adminPanel;
    
    private String userEmail;   // current user
    private boolean isAdmin;    // true if the user is an admin
    
    /** 
     *  Initially displays components allowing the user to connect to the 
	 *	server, and then to login/register.
     */
    public ClientGUI() {        
        // Initialize the frame
        client = new GuestBookClient();
        
        setTitle("Guestbook Client Interface");
        setSize( GUIUtils.CLIENT_SIZE );
        setMinimumSize( GUIUtils.CLIENT_SIZE );
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
                
        // Initialize the panels
        loginPanel = new LoginPanel(this);
        toggleUserFields(false);  
        
        // Display
        add( loginPanel );
        setVisible(true);         
    }    
    
    /** 
     *  Determines what action is taken when an ActionEvent is generated
     *  @param e the generated ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();        
        
        if (source == loginPanel.connectButton) doConnectEvent();
        if (source == loginPanel.loginButton) doLoginEvent();
        if (source == loginPanel.registerButton) doRegisterEvent();
    }
    
    /** 
     *  Attempts to connect to the server. If successful, the login fields are 
     *  activated.
     */
    private void doConnectEvent() {
        try {
            toggleUserFields(false);
            client.setDetails(loginPanel.ipField.getText(), loginPanel.portField.getText());
            
            client.connect();
            if (client.isConnected()) GUIUtils.showInfoMessage(null, "Connected", "Client connected successfully.");
            client.disconnect();
            
            toggleUserFields(true);
        }
        catch (IllegalArgumentException argEx) {
            GUIUtils.showErrorMessage(null, "Invalid Connection Details", "The host IP or port number was invalid.", argEx);
        }
        catch (IOException ioEx) {
            GUIUtils.showErrorMessage(null, "Connection Failed", "A connection could not be established.", ioEx);
        }
    }
    
    /** 
     *  Uses the client to send a login request to the server. If successful, a GuestPanel
     *  is displayed. An AdminPanel is displayed if the user is an admin.
     */
    private void doLoginEvent() {
        if ( loginFieldsAreValid(true) ) {
            try {            
                // Get the email/password
                String email = loginPanel.emailField.getText();
                String password = String.valueOf(loginPanel.passwordField.getPassword());
                int result = client.login( email, password );
                
                // Determine what to do
                switch (result) {
                    case 0:
                        GUIUtils.showErrorMessage(null, "Login Failed", "Username or password was invalid.", null);
                        break;
                    case 1:
                        GUIUtils.showInfoMessage(null, "Login Successfull", "You have successfully logged in.");
                        userEmail = email;
                        isAdmin = false;
                        showServicePanels();
                        break;
                    case 2:
                        GUIUtils.showInfoMessage(null, "Login Successfull", "You have successfully logged in as an administrator.");
                        userEmail = email;
                        isAdmin = true;
                        showServicePanels();
                        break;
                }
            }
            catch (IOException loginEx) {
                GUIUtils.showErrorMessage(null, "Login Failed", "", loginEx);   
            }
            catch (Exception ex) {
                GUIUtils.showErrorMessage(null, "Login Failed", "", ex);
            }
        }
    }
    
    /** 
     *  Uses the client to attempt to register a new user, if the given email is not already taken.
     */
    private void doRegisterEvent() {
        if ( loginFieldsAreValid(false) ) {
            try {                
                String email = loginPanel.emailField.getText();
                
                if ( client.register(email) ) {
                    String successMsg = "You have been registered as a user.\n"
                                      + "Your password is: " + email;
                    GUIUtils.showInfoMessage(null, "Registration Successful", successMsg);
                } 
                
                else {
                    String failMsg = "You could not be registered as a user.\n"
                                   + "There is another user with that email address.";
                    GUIUtils.showErrorMessage(null, "Registration Failed", failMsg, null);
                }
            }
            catch (IOException registerEx) {
                GUIUtils.showErrorMessage(null, "Registration Failed", "", registerEx);   
            }
            catch (Exception ex) {
                GUIUtils.showErrorMessage(null, "Registration Failed", "", ex);
            }
        }
    }
    
    
    /**  
     *  Shows GuestPanel/AdminPanels if a user logs in successfully.
     */
    private void showServicePanels() {
        // Initialize the service panels
        guestPanel = new GuestPanel(client, userEmail);
        if (isAdmin) adminPanel = new AdminPanel(client, userEmail);
        
        // Add the tabbed panels
        JTabbedPane tabbedPanelHolder = new JTabbedPane();
        tabbedPanelHolder.addTab("Guest Services Panel", guestPanel);
        if (isAdmin) tabbedPanelHolder.addTab("Admin Services Panel", adminPanel);        
        
        // Remove the loginPanel and add the tabbedPanelHolder
        getContentPane().remove(loginPanel);
        getContentPane().add(tabbedPanelHolder);
        repaint();
        setVisible(true);
    }
    
    /** 
     *  Checks input for the login fields.
     *  @param checkPassword indicates if the password should be checked (not necessary for
     *  a register event)
     *  @return boolean
     */
    private boolean loginFieldsAreValid(boolean checkPassword) {
        String email = loginPanel.emailField.getText();
        if ( GUIUtils.isNullOrEmpty(email) ) {
            GUIUtils.showErrorMessage(null, "Invalid Email", "Email field cannot be empty", null);
            return false;
        } else if (email.length() > 30) {
            GUIUtils.showErrorMessage(null, "Invalid Email", "Email field cannot be larger than 30 characters.", null);
            return false;
        }
        
        if ( checkPassword ) {
            String pass = String.valueOf( loginPanel.passwordField.getPassword() );
            if ( GUIUtils.isNullOrEmpty(pass) ) {
                GUIUtils.showErrorMessage(null, "Invalid Password", "Password field cannot be empty", null);
                return false;   
            } else if (pass.length() > 30) {
                GUIUtils.showErrorMessage(null, "Invalid Password", "Password field cannot be larger than 30 characters.", null);
                return false;    
            }
        }
               
        return true;        
    }   
    
    /** 
     *  Activates/Deactivates the fields used to log in.
     *  @param toggle toggle value for activation
     */
    private void toggleUserFields(boolean toggle) {
        loginPanel.emailField.setEditable(toggle);
        loginPanel.passwordField.setEditable(toggle);
        loginPanel.loginButton.setEnabled(toggle);
        loginPanel.registerButton.setEnabled(toggle);
    }
    
    
//************************************************************************************
// main()
//************************************************************************************
    public static void main(String[] args) {       
        ClientGUI app = new ClientGUI();
    }
}
