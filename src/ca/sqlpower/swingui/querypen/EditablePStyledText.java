/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Wabit.
 *
 * Wabit is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wabit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.swingui.querypen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.apache.log4j.Logger;

import ca.sqlpower.swingui.querypen.event.ExtendedStyledTextEventHandler;
import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolox.event.PStyledTextEventHandler;
import edu.umd.cs.piccolox.nodes.PStyledText;

/**
 * This class adds an {@link ExtendedStyledTextEventHandler} to the PStyledText
 * to allow editing of the text on clicking the text. A listener can be added
 * to this object to listen for when editing of the text starts and stops.
 * <p>
 * The JTextComponent that is the editing component of this text area is also
 * attached to this extended PStyledText.
 */
public class EditablePStyledText extends PStyledText {
	
	private static final Logger logger = Logger.getLogger(EditablePStyledText.class);
	
	/**
	 * The editor pane shown when the text is clicked. The text entered into this
	 * pane will modify the text shown in this PStyledText.
	 */
	private final JEditorPane editorPane;
	
	/**
	 * An attribute set that contains the font family for lists. This will set the
	 * font of this PStyledText to be a more normal looking font within the app.
	 */
	private final SimpleAttributeSet attributeSet;
	
	/**
	 * This handles the mouse click on the text and shows the editor if the mouse
	 * has actually clicked on the text.
	 */
	private final ExtendedStyledTextEventHandler styledTextEventHandler;
	
	/**
	 * This listener will set the text of this PStyledText and hide the editor
	 * pane when the editor pane loses focus (ie: clicked away from the editor).
	 */
	private FocusListener editorFocusListener = new FocusListener() {
		public void focusLost(FocusEvent e) {
			styledTextEventHandler.stopEditing();
		}
		public void focusGained(FocusEvent e) {
			//do nothing
		}
	};

	/**
	 * The document shared between the editor pane and this PStyledText object.
	 * This will contain the shared text between the two objects and has
	 * the attribute set attached to it.
	 */
	private DefaultStyledDocument doc;

	/**
	 * A list of listeners that fire when this styled text's text is starting or
	 * stopping from being in an editable state.
	 */
	private List<EditStyledTextListener> editingListeners;
	
	/**
	 * Tells the paint method if we should show a border or not. The border will show
	 * when the user has moused over the component.
	 */
	private boolean showHoverBorder;
	
	/**
     * This listener will place an empty string in the where text field if there is no
     * value in the where field. This is done to give the where field a minimum size
     * since the field is always constrained to the text inside it.
     */
    private EditStyledTextListener emptyListener = new EditStyledTextListener() {
        public void editingStopping() {
            fillWithSpaces();
        }
        public void editingStarting() {
            editorPane.setText(editorPane.getText().trim());
        }
    };
    
    /**
     * This is the minimum number of characters that will space out the text field. If
     * there are fewer than this many characters in the text field white space will be
     * appended to the view portion to give users a larger field to click on. This is
     * used instead of defining a minimum width because Piccolo does not have minimum
     * or maximum width values defined internally and the PStyledText recomputes the
     * layout every time the bounds are set.
     */
    protected final int minCharCountSize;
    
    /**
     * This is the canvas this PStyledText node is placed on. The canvas is used to place
     * and position an editor for the PStyledText node so the user can update the text.
     */
    private final PCanvas canvas;

    /**
     * This listener is placed on the camera to listen to changes in the
     * camera's position and move the editor pane accordingly.
     * <p>
     * This is done in support of bug 1720 where the editor pane was not moving
     * when the canvas was being scrolled. The editor pane is placed on the
     * canvas to assure the editor comes in front of all of the layers and is at
     * a size that is human readable. This code, taken from the
     * {@link PStyledTextEventHandler#startEditing(PInputEvent, PStyledText)}
     * method, continually repositions the editor as the camera moves.
     */
    private final PropertyChangeListener cameraViewChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            logger.debug("Property changed for scroll, type: " + evt.getPropertyName());
            Insets pInsets = getInsets();
            Point2D nodePt = new Point2D.Double(getX()+pInsets.left,getY()+pInsets.top);
            localToGlobal(nodePt);
            canvas.getCamera().viewToLocal(nodePt);
            Insets bInsets = editorPane.getBorder().getBorderInsets(editorPane);
            editorPane.setLocation((int)nodePt.getX()-bInsets.left,(int)nodePt.getY()-bInsets.top);
        }
    };

	protected final String startingText;
	
	public EditablePStyledText(QueryPen queryPen, PCanvas canvas) {
		this("", queryPen, canvas);
	}
	
	public EditablePStyledText(String startingText, QueryPen queryPen, PCanvas canvas) {
	    this(startingText, queryPen, canvas, 0);
	}
	
	public EditablePStyledText(String startingText, QueryPen queryPen, final PCanvas canvas, int minCharCountSize) {
	    this.startingText = startingText;
		this.minCharCountSize = minCharCountSize;
	    this.canvas = canvas;
		editorPane = new JEditorPane();
		editingListeners = new ArrayList<EditStyledTextListener>();
		
		doc = new DefaultStyledDocument();
		attributeSet = new SimpleAttributeSet();
		attributeSet.addAttribute(StyleConstants.FontFamily, UIManager.getFont("List.font").getFamily());
		editorPane.setDocument(doc);
		editorPane.setBorder(new LineBorder(editorPane.getForeground()));
		editorPane.setText(startingText);
		doc.setParagraphAttributes(0, editorPane.getText().length(), attributeSet, false);
		setDocument(editorPane.getDocument());
		
		addEditStyledTextListener(emptyListener);

        canvas.getCamera().addPropertyChangeListener(PCamera.PROPERTY_VIEW_TRANSFORM, cameraViewChangeListener);
		
		styledTextEventHandler = new ExtendedStyledTextEventHandler(queryPen, canvas, editorPane) {
		    
			@Override
			public void startEditing(PInputEvent event, PStyledText text) {
				for (EditStyledTextListener l : editingListeners) {
					l.editingStarting();
				}
				super.startEditing(event, text);
			}
			
			@Override
			public void stopEditing() {
				editorPane.setText(editorPane.getText().replaceAll("\n", "").trim());
				fillWithSpaces();
				syncWithDocument();
				for (EditStyledTextListener l : editingListeners) {
					l.editingStopping();
				}
				super.stopEditing();
				logger.debug("Editing stopped.");
			}
		};
		addInputEventListener(styledTextEventHandler);
		
		editorPane.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {
				//Do nothing
			}
			public void keyReleased(KeyEvent e) {
				//Do nothing
			}
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					styledTextEventHandler.stopEditing();
				}
			}
		});
		
		editorPane.addFocusListener(editorFocusListener);
		
		addInputEventListener(new PBasicInputEventHandler() {
			
			@Override
			public void mouseEntered(PInputEvent event) {
				showHoverBorder = true;
				repaint();
			}
			
			@Override
			public void mouseExited(PInputEvent event) {
				showHoverBorder = false;
				repaint();
			}
		});
		
	}
	
	@Override
	protected void paint(PPaintContext paintContext) {
		super.paint(paintContext);
		Graphics2D g = paintContext.getGraphics();
		Paint oldPaint = g.getPaint();
		Stroke oldStroke = g.getStroke();
		g.setStroke(new BasicStroke(1));
		g.setPaint(Color.GRAY);
		if (showHoverBorder) {
			g.drawRect((int) getBounds().getX(), (int) getBounds().getY(), (int) getBounds().getWidth() , (int) getBounds().getHeight());
		}
		g.setStroke(oldStroke);
		g.setPaint(oldPaint);
	}
	
	/**
	 * This will allow classes extending this class to get the focus listener that
	 * will stop the edit when focus is lost. Other classes may want to define different
	 * behaviour when focus is lost.
	 */
	protected FocusListener getEditorFocusListener() {
		return editorFocusListener;
	}
	
	/**
	 * Allows classes extending this class to get the styledTextEventHandler that
	 * handles text replacements for this class.
	 */
	protected ExtendedStyledTextEventHandler getStyledTextEventHandler() {
		return styledTextEventHandler;
	}
	
	public void addEditStyledTextListener(EditStyledTextListener l) {
		editingListeners.add(l);
	}
	
	public void removeEditStyledTextListener(EditStyledTextListener l) {
		editingListeners.add(l);
	}
	
	public JEditorPane getEditorPane() {
		return editorPane;
	}

	private void fillWithSpaces() {
		if (editorPane.getText() == null || editorPane.getText().trim().equals("") ) {
			editorPane.setText(startingText);
			syncWithDocument();
		} else if (	editorPane.getText().length() < minCharCountSize) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < minCharCountSize - editorPane.getText().length(); i++) {
				sb.append(" ");
			}
			editorPane.setText(editorPane.getText() + sb.toString());
			syncWithDocument();
		}
	}
	
}
