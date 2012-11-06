
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

import java.awt.Dimension;
import java.awt.FlowLayout;


/** 
 *  The ClientGUI displays a LoginPanel on startup. The LoginPanel
 *  allows a user to:
 *              - Connect to the server
 *              - Log in
 *              - Register a new email
 *              
 *  @author Joram Benham 200801274
 *  @version April 8, 2011
 */
public class LoginPanel extends JPanel {
    private ClientGUI parent;   // reference to the parent GUI    
    
    JTextField emailField;
    JPasswordField passwordField;
    JTextField ipField;
    JTextField portField;
    
    JButton loginButton;        // click to login
    JButton registerButton;     // click to register
    JButton connectButton;      // click to connect
    
    /** Constructor
     * @param parentGUI reference to the parent container; the LoginPanel uses its
     * parent container to handle ActionEvents
     */
    public LoginPanel(ClientGUI parentGUI) {
        
        // Initialize the panel
        if (parentGUI == null) throw new NullPointerException("Parent cannot be null.");
        parent = parentGUI;
        setLayout( new FlowLayout() );
        setBackground( GUIUtils.BG_COLOR );
        
        // Initialize the fields
        int fieldSize = 20;
        emailField = new JTextField( fieldSize );
        passwordField = new JPasswordField( fieldSize );
        ipField = new JTextField( "localhost", fieldSize );
        portField = new JTextField( "9000", fieldSize );
        
        // Initialize the buttons
        loginButton = new JButton( "Login" );
        loginButton.addActionListener( parent );
        registerButton = new JButton( "Register" );
        registerButton.addActionListener( parent );
        connectButton = new JButton( "Connect" );
        connectButton.addActionListener( parent );
        
        // Add the components
        Box contentBox = Box.createVerticalBox();
        Dimension gap = new Dimension(5, 20);
        Dimension maxFieldSize = new Dimension(100, 30);
        GUIUtils.addBoxRow( contentBox, new JComponent[]{ GUIUtils.createHeader("Guestbook Client Login") },
                            maxFieldSize, gap );
        GUIUtils.addBoxRow( contentBox, new JComponent[]{ GUIUtils.createTitle("Server Host (name or IP)"), ipField },
                            maxFieldSize, gap );
        GUIUtils.addBoxRow( contentBox, new JComponent[]{ GUIUtils.createTitle("Server Port"), portField },
                            maxFieldSize, new Dimension(5, 35) );
        GUIUtils.addBoxRow( contentBox, new JComponent[]{ connectButton },
                            maxFieldSize, new Dimension(15, 100) );
        GUIUtils.addBoxRow( contentBox, new JComponent[]{ GUIUtils.createTitle("Email"), emailField },
                            maxFieldSize, gap );
        GUIUtils.addBoxRow( contentBox, new JComponent[]{ GUIUtils.createTitle("Password"), passwordField },
                            maxFieldSize, gap );
        GUIUtils.addBoxRow( contentBox, new JComponent[]{ loginButton, registerButton },
                            maxFieldSize, new Dimension(15, 0) );
                            
        // Display
        add( contentBox );
        setVisible(true);
    }
}
