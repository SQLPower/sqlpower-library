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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.log4j.Logger;

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
	private final JLabel previewLabel;
	private final JLabel varPreviewLabel1;
	private final JLabel varPreviewLabel2;
	private boolean stuffToInsert = true;
	private final VariableInserter action;
	private final JLabel optionsLabel;
	private final JLabel namespaceLabel;
	private final JCheckBox namespaceBox;
	private final JLabel generalLabel;
	private static final Logger logger = Logger.getLogger(VariablesPanel.class);
	
	public VariablesPanel(
			SPVariableHelper variableHelper,
    		VariableInserter action) 
	{
		this(variableHelper,action,"");
	}
	
	/**
	 * Default constructor for the variables panel.
	 * @param variableHelper A helper that will be used in order to
	 * resolve discover and resolve variables.
	 * @param action An implementation of {@link VariableInserter} that 
	 * gets called once the variable has been created. This action will be executed
	 * on the Swing Event Dispatch Thread.
	 * @param varDefinition The default variable definition string.
	 */
	public VariablesPanel(
			SPVariableHelper variableHelper,
    		VariableInserter action,
    		String varDefinition) 
	{
		this.variableHelper = variableHelper;
		this.action = action;
	
		this.generalLabel = new JLabel("General");
		this.generalLabel.setFont(this.generalLabel.getFont().deriveFont(Font.BOLD));
		
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
		
		this.optionsLabel = new JLabel("Options");
		this.optionsLabel.setFont(this.optionsLabel.getFont().deriveFont(Font.BOLD));
		
		this.varDefaultLabel = new JLabel("Default value");
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
		
		this.varEditLabel = new JLabel("Customization");
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
						if (SPVariableHelper.getNamespace(text) == null) {
							namespaceBox.setSelected(false);
						} else {
							namespaceBox.setSelected(true);
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
		
		this.namespaceLabel = new JLabel("Constrain to namespace");
		this.namespaceBox = new JCheckBox("");
		if (SPVariableHelper.getNamespace(varDefinition) != null || "".equals(varDefinition)) {
			this.namespaceBox.setSelected(true);
		} else {
			this.namespaceBox.setSelected(false);
		}
		this.namespaceBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateGUI();
			}
		});
		
		this.previewLabel = new JLabel("Preview");
		this.previewLabel.setFont(this.previewLabel.getFont().deriveFont(Font.BOLD));
		this.varPreviewLabel1 = new JLabel("Current value is");
		this.varPreviewLabel1.setForeground(Color.GRAY);
		this.varPreviewLabel2 = new JLabel();
		this.varPreviewLabel2.setForeground(Color.GRAY);
		
		
		this.panel = new JPanel(new MigLayout());
		
		this.panel.add(this.generalLabel, "growx, span, wrap");
		this.panel.add(new JLabel(" "), "wmin 20, wmax 20");
		this.panel.add(this.pickerLabel);
		this.panel.add(this.varNameText, "growx, wmin 275, wmax 275, gapright 0");
		this.panel.add(this.varPicker, "wmax 20, hmax 20, wrap, gapleft 0");
		
		this.panel.add(this.optionsLabel, "growx, span, wrap");
		
		this.panel.add(new JLabel(" "), "wmin 20, wmax 20");
		this.panel.add(this.varDefaultLabel);
		this.panel.add(this.varDefaultText, "span, wrap, wmin 300, wmax 300");

		this.panel.add(new JLabel(" "), "wmin 20, wmax 20");
		this.panel.add(namespaceLabel);
		this.panel.add(namespaceBox, "span, wrap");
		
		this.panel.add(new JLabel(" "), "wmin 20, wmax 20");
		this.panel.add(this.varEditLabel);
		this.panel.add(this.varEditText, "span, wmin 300, wmax 300, wrap");
		
		this.panel.add(this.previewLabel, "growx, span, wrap");
		
		this.panel.add(new JLabel(" "), "wmin 20, wmax 20");
		this.panel.add(this.varPreviewLabel1);
		this.panel.add(this.varPreviewLabel2, "span, growx");
		
		this.currentPickedVariable = varDefinition;
		updateGUI();
	}
	
	private void updateGUI() 
	{
		String text = "${";
		if (this.namespaceBox.isSelected()) {
			text += currentPickedVariable;
		} else {
			text += SPVariableHelper.getKey(currentPickedVariable);
		}
		if (!currentDefValue.trim().equals("")) {
			text += SPVariableHelper.DEFAULT_VALUE_DELIMITER;
			text += currentDefValue;
		}
		text += "}";
		this.varNameText.setText(currentPickedVariable);
		this.varDefaultText.setText(currentDefValue);
		this.varEditText.setText(text);
		
		Object resolvedValue = variableHelper.resolve(currentPickedVariable, currentDefValue == null ? "null" : currentDefValue);	
		this.varPreviewLabel2.setText(resolvedValue == null ? "null" : resolvedValue.toString());
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
    	final MultiValueMap namespaces = this.variableHelper.getNamespaces();
    	
		List<String> sortedNames = new ArrayList<String>(namespaces.keySet().size());
		sortedNames.addAll(namespaces.keySet());
		Collections.sort(sortedNames, new Comparator<String>() {
			public int compare(String o1, String o2) {
				if (o1 == null) {
					return -1;
				}
				if (o2 == null) {
					return 1;
				}
				return o1.compareTo(o2);
			};
		});
		
    	final JPopupMenu menu = new JPopupMenu();
        for (final String name : sortedNames) {
        	final JMenu subMenu = new JMenu(name);
    		menu.add(subMenu);
    		subMenu.addMenuListener(new MenuListener() {
				private Timer timer;
				public void menuSelected(MenuEvent e) {
					
					subMenu.removeAll();
					subMenu.add(new PleaseWaitAction());

					ActionListener menuPopulator = new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							if (subMenu.isPopupMenuVisible()) {
								subMenu.removeAll();
								for (Object namespaceO : namespaces.getCollection(name)) {
									String namespace = (String)namespaceO;
									logger.debug("Resolving variables for namespace ".concat(namespace));
									int nbItems = 0;
									for (String key : variableHelper.keySet(namespace)) {
										subMenu.add(new InsertVariableAction(SPVariableHelper.getKey((String)key), (String)key));
										nbItems++;
									}
									if (nbItems==0) {
										subMenu.add(new DummyAction());
										logger.debug("No variables found.");
									}
								}
								subMenu.revalidate();
								subMenu.getPopupMenu().pack();
							}
						}
					};
					timer = new Timer(700, menuPopulator);
					timer.setRepeats(false);
					timer.start();
				}
				public void menuDeselected(MenuEvent e) {
					timer.stop();
				}
				public void menuCanceled(MenuEvent e) {
					timer.stop();
				}
			});
        }
    	
        menu.show(varNameText, 0, varNameText.getHeight());
    }
	
	private final class DummyAction extends AbstractAction {
		public DummyAction() {
			super("No variables available");
			this.setEnabled(false);
		}
		public void actionPerformed(ActionEvent e) {
		}
	}
	
	private final class PleaseWaitAction extends AbstractAction {
		public PleaseWaitAction() {
			super("Please wait...");
			this.setEnabled(false);
		}
		public void actionPerformed(ActionEvent e) {
		}
	}
	
//	public static void main(String[] args) {
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                try {
//                	
//                	JFrame f = new JFrame("TEST PANEL");
//                    JPanel outerPanel = new JPanel(new BorderLayout());
//                    outerPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
//                	
//                	MockSPObject root = new MockSPObject("root");
//                	root.setResolver(new SPSimpleVariableResolver(root, root.getUUID(), root.getName()));
//                	root.begin("Begin tree building...");
//            		MockSPObject node1 = new MockSPObject("node1");
//            		MockSPObject node2 = new MockSPObject("node2");
//            		MockSPObject node3 = new MockSPObject("node3");
//            		root.addChild(node1, 0);
//            		root.addChild(node2, 1);
//            		node2.addChild(node3, 0);
//            		root.commit();
//
//            		node1.getVariableResolver().store("key1", "value1");
//            		node2.getVariableResolver().store("key2", "value2");
//            		node3.getVariableResolver().store("key3", "value3");
//            		node3.getVariableResolver().store("key4", "value4");
//            		
//            		SPVariableHelper helper = new SPVariableHelper(node3);
//                	
//            		VariablesPanel panel = new VariablesPanel(helper, new VariableInserter() {
//						public void insert(String variable) {
//						}
//					});
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
//		private SPSimpleVariableResolver resolver = null;
//		public MockSPObject(String name) {
//			this.setName(name);
//		}
//		@Override
//		public void setParent(SPObject parent) {
//			super.setParent(parent);
//			this.resolver = new SPSimpleVariableResolver(this, this.getUUID(), this.getName());
//		}
//		public void setResolver(SPSimpleVariableResolver resolver) {
//			this.resolver = resolver;
//		}
//		protected boolean removeChildImpl(SPObject child) {
//			return this.children.remove(child);
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
