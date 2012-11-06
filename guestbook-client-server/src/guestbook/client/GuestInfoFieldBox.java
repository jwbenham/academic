
package guestbook.client;

import guestbook.server.RealDatabaseHandler;
import guestbook.shared.*;

import javax.swing.JComponent;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPanel;

import java.awt.Dimension;
import java.awt.FlowLayout;

/** 
 *  The ClientGUI displays fields for a GuestInfo object in
 *  multiple places. A GuestInfoFieldBox allows the GUI to
 *  quickly make a panel holding GuestInfo fields, to parse the
 *  data from the fields and create a GuestInfo object.
 *              
 *  @author Joram Benham 200801274
 *  @version April 8, 2011
 */
public class GuestInfoFieldBox extends JPanel {
    private String userEmail;
    private boolean showEmail;
    
    private JTextField emailField;
    private JTextField passwordField;
    private JTextField nameField;
    private JTextField addressField;
    private JTextField cityField;
    private JTextField postcodeField;
    private JTextField telephoneField;    
    
    /** Constructor
     * @param theEmail email of the currently logged in user
     * @param showEmailField indicates whether or not the emailField should be displayed
     */
    public GuestInfoFieldBox(String theEmail, boolean showEmailField) { 
        userEmail = theEmail;
        showEmail = showEmailField;
        setBackground( GUIUtils.BG_COLOR );
        setMaximumSize( new Dimension(600, 200) );
        
        // Initialize fields
        int size = 15;
        emailField = new JTextField(size);
        passwordField = new JTextField(size);
        nameField = new JTextField(size);
        addressField = new JTextField(size);
        cityField = new JTextField(size);
        postcodeField = new JTextField(size);
        telephoneField = new JTextField(size);
        
        // Add components
        Box content = Box.createVerticalBox();
        
        Dimension maxSize = new Dimension(100, 20);
        Dimension gap = new Dimension(15, 5);
        content.add( Box.createVerticalStrut(5) );
        
        if (showEmailField) {
            GUIUtils.addBoxRow( content,
                                new JComponent[]{ GUIUtils.createTitle("Email"), emailField},
                                maxSize, gap );    
        }
        
        GUIUtils.addBoxRow( content,
                            new JComponent[]{ GUIUtils.createTitle("Password"), passwordField},
                            maxSize, gap );
        GUIUtils.addBoxRow( content,
                            new JComponent[]{ GUIUtils.createTitle("Name"), nameField },
                            maxSize, gap );
        GUIUtils.addBoxRow( content,
                            new JComponent[]{ GUIUtils.createTitle("Address"), addressField },
                            maxSize, gap );
        GUIUtils.addBoxRow( content,
                            new JComponent[]{ GUIUtils.createTitle("City"), cityField },
                            maxSize, gap );
        GUIUtils.addBoxRow( content,
                            new JComponent[]{ GUIUtils.createTitle("Postcode"), postcodeField },
                            maxSize, gap );
        GUIUtils.addBoxRow( content,
                            new JComponent[]{ GUIUtils.createTitle("Telephone"), telephoneField },
                            maxSize, new Dimension(5, 5) );
                            
        // Display
        add(content);
        setVisible(true);
    }
    
    /** 
     * Clears the JTextFields
     */
    public void clearFields() {
        emailField.setText("");
        nameField.setText("");
        addressField.setText("");
        cityField.setText("");
        postcodeField.setText("");
        telephoneField.setText("");
        passwordField.setText("");  
    }
    
    /** 
     * Sets the fields to hold the information of a GuestInfo object
     * @param info the information to fill the fields with
     */
    public void setGuestInfo(GuestInfo info) {       
        emailField.setText(info.getEmail());
        nameField.setText(info.getName());
        addressField.setText(info.getAddress());
        cityField.setText(info.getCity());
        postcodeField.setText(info.getPostcode());
        telephoneField.setText(info.getTelephone());
        passwordField.setText(info.getPassword());      
    }
    
    /** 
     * Parses the fields for the data to create a GuestInfo object
     * @return GuestInfo object with the data from the fields
     */
    public GuestInfo getGuestInfo() {
        GuestInfo infoFromFields;
        String theEmail;
        
        if (showEmail) {
            // If the user was able to input an email but left it blank, we don't want
            // to use the email as a criteria for anything. Since the email is necessary 
            // for all GuestInfo, use a DUMMY_EMAIL which will be ignored by the database.
            String inputEmail = emailField.getText();
            if (inputEmail.equals("")) theEmail = RealDatabaseHandler.DUMMY_EMAIL;
            else theEmail = inputEmail;
        }
        else theEmail = userEmail;
        
        infoFromFields = new GuestInfo( nameField.getText(),
                                        addressField.getText(),
                                        cityField.getText(),
                                        postcodeField.getText(),
                                        telephoneField.getText(),
                                        theEmail,
                                        passwordField.getText() );
        return infoFromFields;
    }
    
    /**
     * Sets whether or not the fields are editable
     * @param editable indicates whether fields should be editable or not
     */
    public void setEditable( boolean editable ) {
        emailField.setEditable(editable);
        passwordField.setEditable(editable);
        nameField.setEditable(editable);
        addressField.setEditable(editable);
        cityField.setEditable(editable);
        postcodeField.setEditable(editable);
        telephoneField.setEditable(editable);
    }
}
