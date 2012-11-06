
package guestbook.shared;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Color;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

/** 
 *   GUIUtils provides configuration information and
 *   utility methods for GUI classes.
 *              
 *  @author Joram Benham 200801274
 *  @version April 6, 2011
 */
public class GUIUtils {

    public static final Dimension MIN_SIZE = new Dimension( 640, 480 );
    public static final Dimension CLIENT_SIZE = new Dimension( 1024, 700 );
    
    public static final Color BG_COLOR = Color.BLACK;
    
    public static final Font HEADER_FONT = new Font(Font.MONOSPACED, Font.BOLD, 18);
    public static final Color HEADER_COLOR = Color.LIGHT_GRAY;
    
    public static final Font SUBHEADER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    public static final Color SUBHEADER_COLOR = new Color(150, 220, 220);
        
    public static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    public static final Color TITLE_COLOR = Color.WHITE;
    
    public static final Font TEXT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    public static final Color TEXT_COLOR = Color.BLACK;
 
    /** 
     *  Creates and returns a JLabel with the header font and color.
     *  @param header String indicating the header
     *  @return JLabel formatted to spec
     */
    public static JLabel createHeader(String header) {
        return createStyledLabel(header, HEADER_FONT, HEADER_COLOR);
    }
    
    /** 
     *  Creates and returns a JLabel with the subheader font and color.
     *  @param header String indicating the header
     *  @return JLabel formatted to spec
     */
    public static JLabel createSubHeader(String header) {
        return createStyledLabel(header, SUBHEADER_FONT, SUBHEADER_COLOR);
    }
    
    /** e
     *  Creates and returns a JLabel with the title font and color.
     *  @param title String indicating the title
     *  @return JLabel formatted to spec
     */
    public static JLabel createTitle(String title) {
        return createStyledLabel(title, TITLE_FONT, TITLE_COLOR);
    }
    
    /** 
     *  Creates a JLabel with the specified Font and Color
     *  @param t the title
     *  @param f the Font to use
     *  @param c the Color to use
     *  @return JLabel formatted with the parameters
     */
    public static JLabel createStyledLabel(String t, Font f, Color c) {
        JLabel label = new JLabel(t);
        label.setFont(f);
        label.setForeground(c);
        return label;
    }
    
    /** 
     *  Adds a HorizontalBox containing a "line" of JComponents to the indicated
     *  parent container.
     *  @param parent the JComponent to add the row to
     *  @param items the JComponents to add to parent
     *  @param maxSize the maximum size of an item in the row
     *  @param gap the gap between rows and items in a row
     */
    public static void addBoxRow(JComponent parent, JComponent[] items, Dimension maxSize, Dimension gap) {
        Box row = Box.createHorizontalBox();
        
        for (int i = 0; i < items.length; i++) {
            items[i].setMaximumSize( maxSize );
            row.add( items[i] );
            if (i != items.length-1) {
                row.add( Box.createHorizontalGlue() );
                row.add( Box.createHorizontalStrut( (int)gap.getWidth() ) );
            }
        }
        
        parent.add( row );
        parent.add( Box.createVerticalStrut( (int)gap.getHeight() ) );
    }
    
    /** 
     *  Uses a JOptionPane to display a graphical error message to the user.
     *  @param src indicates the source component for the message
     *  @param t the message title
     *  @param msg the user-defined message
     *  @param ex the Exception which caused the error message, if available
     */
    public static void showErrorMessage(JComponent src, String t, String msg, Exception ex ) {
        String fullMessage = "MESSAGE:\n" + msg;
        if (ex != null) fullMessage += "\n\nDETAILS:\n" + ex + "\n\nCAUSE:\n" + ex.getCause();
        JOptionPane.showMessageDialog( src, fullMessage, t, JOptionPane.ERROR_MESSAGE );
    }
    
    /** 
     *  Uses a JOptionPane to display a graphical information message to the user.
     *  @param src indicates the source component for the message
     *  @param t the message title
     *  @param msg the user-defined message
     */
    public static void showInfoMessage(JComponent src, String t, String msg) {
        String fullMessage = "MESSAGE:\n" + msg;
        JOptionPane.showMessageDialog( src, fullMessage, t, JOptionPane.INFORMATION_MESSAGE );
    }
    
    /** 
     *  Tests if a String reference is null, or equals ""
     *  @param str the string to test
     *  @return boolean true if the string is null or empty
     */
    public static boolean isNullOrEmpty( String str ) { return (str == null || str.equals("")); }
    
}
