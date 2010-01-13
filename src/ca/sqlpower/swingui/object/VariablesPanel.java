/*
 * Copyright (c) 2010, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.swingui.object;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.collections.map.MultiValueMap;

import ca.sqlpower.object.SPVariableHelper;
import ca.sqlpower.swingui.DataEntryPanel;

/**
 * This calss displays a panel to pick and insert a variable.
 * @author Luc Boudreau
 */
public class VariablesPanel implements DataEntryPanel {
	
	private final JPanel panel;
	private String currentPickedVariable = "";
	private String currentDefValue = "";
	private final SPVariableHelper variableHelper;
	private final JLabel pickerLabel;
	private final JTextField varNameText;
	private final JButton varPicker;
	private final JLabel varDefaultLabel;
	private final JTextField varDefaultText;
	private final JLabel varEditLabel;
	private final JTextField varEditText;
	private final JLabel varPreviewLabel1;
	private final JLabel varPreviewLabel2;
	private boolean stuffToInsert = true;
	private final VariableInsertionCallback action;
	private final String namespace;
	
	/**
	 * Default constructor for the variables panel.
	 * @param variableHelper A helper that will be used in order to
	 * resolve discover and resolve variables.
	 * @param namespace The namespacec into which to resolve variables.
	 * Pass null to resolve all available variables.
	 * @param action An implementation of {@link VariableInsertionCallback} that 
	 * gets called once the variable has been created. This action will be executed
	 * on the Swing Event Dispatch Thread.
	 */
	public VariablesPanel(
			SPVariableHelper variableHelper,
			String namespace,
    		VariableInsertionCallback action) 
	{
		this.variableHelper = variableHelper;
		this.namespace = namespace;
		this.action = action;
		
		this.pickerLabel = new JLabel("Pick a variable");
		this.varNameText = new JTextField();
		this.varNameText.setEditable(false);
		this.varNameText.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent e) {
			}
			public void mousePressed(MouseEvent e) {
			}
			public void mouseExited(MouseEvent e) {
			}
			public void mouseEntered(MouseEvent e) {
			}
			public void mouseClicked(MouseEvent e) {
				ShowPickerAction act = new ShowPickerAction();
				act.actionPerformed(null);
			}
		});
		this.varPicker = new JButton(new ShowPickerAction());
		
		this.varDefaultLabel = new JLabel("Default value (optional)");
		this.varDefaultText = new JTextField();
		this.varDefaultText.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						currentDefValue = varDefaultText.getText();
						updateGUI();
					}
				});
			}
			public void keyReleased(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
			}
		});
		
		this.varEditLabel = new JLabel("Customization (optional)");
		this.varEditText = new JTextField();
		this.varEditText.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						int carPos = varEditText.getCaretPosition();
						String text = varEditText.getText().replace("$", "").replace("{", "").replace("}", "");
						currentPickedVariable = SPVariableHelper.stripDefaultValue(text);
						if (currentPickedVariable==null) {
							currentPickedVariable = "";
						}
						currentDefValue = SPVariableHelper.getDefaultValue(text);
						if (currentDefValue==null) {
							currentDefValue = "";
						}
						updateGUI();
						try {
							varEditText.setCaretPosition(carPos);
						} catch (IllegalArgumentException e) {
							varEditText.setCaretPosition(carPos-1);
						}
					}
				});
			}
			public void keyReleased(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
			}
		});
		
		this.varPreviewLabel1 = new JLabel("Current value : ");
		this.varPreviewLabel1.setForeground(Color.GRAY);
		this.varPreviewLabel2 = new JLabel();
		this.varPreviewLabel2.setForeground(Color.GRAY);
		
		
		this.panel = new JPanel(new MigLayout());
		
		//this.panel.add(topLabel, "span, wrap");
		this.panel.add(this.pickerLabel);
		this.panel.add(this.varNameText, "growx, wmin 175, wmax 175, gapright 0");
		this.panel.add(this.varPicker, "wmax 20, hmax 20, wrap, gapleft 0");
		
		this.panel.add(new JSeparator(), "growx, span, wrap, gaptop 10px, gapbottom 10px");
		
		this.panel.add(this.varDefaultLabel);
		this.panel.add(this.varDefaultText, "span, wrap, wmin 200, wmax 200");
		
		this.panel.add(this.varEditLabel);
		this.panel.add(this.varEditText, "span, wmin 200, wmax 200, wrap");
		
		this.panel.add(new JSeparator(), "growx, span, wrap, gaptop 10px, gapbottom 10px");
		
		this.panel.add(this.varPreviewLabel1);
		this.panel.add(this.varPreviewLabel2, "span, growx");
		
		updateGUI();
	}
	
	private void updateGUI() 
	{
		String text = "${";
		text += currentPickedVariable;
		if (!currentDefValue.trim().equals("")) {
			text += SPVariableHelper.DEFAULT_VALUE_DELIMITER;
			text += currentDefValue;
		}
		text += "}";
		this.varNameText.setText(currentPickedVariable);
		this.varDefaultText.setText(currentDefValue);
		this.varEditText.setText(text);
		this.varPreviewLabel2.setText(" " + variableHelper.resolve(currentPickedVariable, currentDefValue.equals("") ? "null" : currentDefValue).toString());
	}
	
	private final class ShowPickerAction extends AbstractAction {
		public ShowPickerAction() {
			super("...");
		}
		public void actionPerformed(ActionEvent e) {
			showVarsPicker();
		}
	}
	
	private final class InsertVariableAction extends AbstractAction {
		private final String var;
		public InsertVariableAction(String label, String var) {
			super(label);
			this.var = var;
		}
		public void actionPerformed(ActionEvent e) {
			currentPickedVariable = var;
			updateGUI();
		}
	}

	public boolean applyChanges() {
		final String variable = varEditText.getText();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				action.insert(variable);
			}
		});
		this.stuffToInsert = false;
		return true;
	}

	public void discardChanges() {
		this.stuffToInsert = false;
	}

	public JComponent getPanel() {
		return this.panel;
	}

	public boolean hasUnsavedChanges() {
		return this.stuffToInsert;
	}
	
	@SuppressWarnings("unchecked")
	private void showVarsPicker() 
    {
    	MultiValueMap keys = new MultiValueMap();
		variableHelper.recursiveKeySet(
				variableHelper.getContextSource(),
				keys, 
				this.namespace,
				true);
		
		List<String> sortedNames = new ArrayList<String>(keys.keySet().size());
		sortedNames.addAll(keys.keySet());
		Collections.sort(sortedNames, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			};
		});
		
    	JPopupMenu menu = new JPopupMenu();
        for (String name : sortedNames) {
        	JMenu subMenu = new JMenu(name.toString());
    		menu.add(subMenu);
    		for (Object key : keys.getCollection(name)) {
    			subMenu.add(new InsertVariableAction(SPVariableHelper.getKey((String)key), (String)key));
    		}
        }
    	
        menu.show(varNameText, 0, varNameText.getHeight());
    }
	
//	public static void main(String[] args) {
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                try {
//                	
//                	JFrame f = new JFrame("TEST PANEL");
//                    JPanel outerPanel = new JPanel(new BorderLayout());
//                    outerPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
//                    JTextField dummyField = new JTextField();
//                	
//                	MockSPObject root = new MockSPObject();
//            		MockSPObject node1 = new MockSPObject();
//            		MockSPObject node2 = new MockSPObject();
//            		MockSPObject node3 = new MockSPObject();
//            		root.addChild(node1, 0);
//            		root.addChild(node2, 1);
//            		node2.addChild(node3, 0);
//            		
//            		SPVariableHelper helper = new SPVariableHelper(node1);
//            		helper.setWalkDown(true);
//            		
//            		node1.getVariableResolver().setNamespace("namespace");
//            		node1.getVariableResolver().setSnobbyResolver(false);
//            		node1.getVariableResolver().store("key", "value");
//            		
//            		node3.getVariableResolver().setNamespace("namespace3");
//            		node3.getVariableResolver().setSnobbyResolver(false);
//            		node3.getVariableResolver().store("key3", "value3");
//            		
//            		
//                	
//            		VariablesPanel panel = new VariablesPanel(helper, f, dummyField);
//                	
//                    
//                    outerPanel.add(panel.getPanel(), BorderLayout.CENTER);
//                    f.setContentPane(outerPanel);
//                    f.pack();
//                    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//                    f.setVisible(true);
//                    
//                } catch (Exception ex) {
//                    throw new RuntimeException(ex);
//                }
//            }
//        });
//    }
//	
//	private static class MockSPObject extends AbstractSPObject implements SPVariableResolverProvider {
//		private List<SPObject> children = new ArrayList<SPObject>();
//		private SPSimpleVariableResolver resolver;
//		public MockSPObject() {
//			this.resolver = new SPSimpleVariableResolver(null);
//		}
//		@Override
//		public String getName() {
//			return this.uuid;
//		}
//		protected boolean removeChildImpl(SPObject child) {
//			return true;
//		}
//		public boolean allowsChildren() {
//			return true;
//		}
//		public int childPositionOffset(Class<? extends SPObject> childType) {
//			return 0;
//		}
//		public List<? extends SPObject> getChildren() {
//			return this.children;
//		}
//		public List<? extends SPObject> getDependencies() {
//			return Collections.emptyList();
//		}
//		public void removeDependency(SPObject dependency) {
//			return;
//		}
//		public SPSimpleVariableResolver getVariableResolver() {
//			return this.resolver;
//		}
//		@Override
//		protected void addChildImpl(SPObject child, int index) {
//			this.children.add(child);
//		}
//		public List<Class<? extends SPObject>> getAllowedChildTypes() {
//			List<Class<? extends SPObject>> types = new ArrayList<Class <? extends SPObject>>();
//			types.add(SPObject.class);
//			return types;
//		}
//	}
}
