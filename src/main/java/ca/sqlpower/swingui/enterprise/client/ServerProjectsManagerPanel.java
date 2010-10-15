/*
 * Copyright (c) 2010, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.swingui.enterprise.client;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.http.client.CookieStore;
import org.json.JSONException;

import ca.sqlpower.enterprise.ClientSideSessionUtils;
import ca.sqlpower.enterprise.client.ProjectLocation;
import ca.sqlpower.enterprise.client.SPServerInfo;
import ca.sqlpower.enterprise.client.SPServerInfoManager;
import ca.sqlpower.swingui.SPSUtils;
import ca.sqlpower.util.UserPrompterFactory;
import ca.sqlpower.util.UserPrompter.UserPromptOptions;
import ca.sqlpower.util.UserPrompter.UserPromptResponse;
import ca.sqlpower.util.UserPrompterFactory.UserPromptType;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This class can create a dialog to display server connections and the projects
 * in them.
 */
public abstract class ServerProjectsManagerPanel {

    private final Component dialogOwner;
    private final UserPrompterFactory upf;
    private final File defaultFileDirectory;
    
    private final JPanel panel;
    private final Action closeAction;
    private final JList projects;
    private final JList servers;
    
    private final Action refreshAction = new AbstractAction("Refresh") {
        public void actionPerformed(ActionEvent e) {
            refreshInfoList();
        }
    };
    
    private final Action newAction = new AbstractAction("New...") {
        public void actionPerformed(ActionEvent e) {
         
            if (getSelectedServerInfo() != null) {
               
                String name = JOptionPane.showInputDialog(dialogOwner, "Please specify the name of your project", "", JOptionPane.QUESTION_MESSAGE);
                
                if (name != null) {
                    DefaultListModel model = (DefaultListModel) projects.getModel();
                    for (int i = 0; i < model.size(); i++) {
                        if (((ProjectLocation) model.getElementAt(i)).getName().trim().equalsIgnoreCase(name.trim())) {
                            JOptionPane.showMessageDialog(dialogOwner, "A project called \"" + name + "\" already exists. Please use a different name.", "Duplicate project name", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }

                    try {
                        Window frame = SwingUtilities.getWindowAncestor(dialogOwner);
                        
                        JLabel messageLabel = new JLabel("Creating New Project");
                        JProgressBar progressBar = new JProgressBar();
                        progressBar.setIndeterminate(true);
                        
                        final JDialog dialog = new JDialog(frame, "Creating New Project");
                        
                        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("pref:grow, 5dlu, pref"));
                        builder.setDefaultDialogBorder();
                        builder.append(messageLabel, 3);
                        builder.nextLine();
                        builder.append(progressBar, 3);
                        dialog.add(builder.getPanel());
                        
                        dialog.setSize(new Dimension(300,90));
                        dialog.setLocationRelativeTo(frame);
                        dialog.setAlwaysOnTop(true);
                        dialog.setVisible(true);
                        
                        ClientSideSessionUtils.createNewServerSession(getSelectedServerInfo(), name, cookieStore, upf);
 
                        dialog.dispose();
                        
                    } catch (Exception ex) {
                        throw new RuntimeException("Unable to create new project", ex);
                    }

                    refreshInfoList();
                }
            }
        }
    };

    private final Action uploadAction = new AbstractAction("Upload") {
        
        public void actionPerformed(ActionEvent e) {
            final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(dialogOwner), "Upload Project", ModalityType.DOCUMENT_MODAL);
            
            JPanel panel = new JPanel();
            FormLayout layout = new FormLayout("4dlu, pref, 4dlu, max(150dlu;pref), 4dlu, pref, 4dlu", "pref, pref, pref");
            DefaultFormBuilder builder = new DefaultFormBuilder(layout, panel);
            CellConstraints cc = new CellConstraints();
            
            final JTextField nameField = new JTextField();
            builder.add(new JLabel("Name"), cc.xy(2, 1));
            builder.add(nameField, cc.xyw(4, 1, 3));
            
            final JTextField fileField = new JTextField();
            JButton fileButton = new JButton("...");
            fileButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser chooser = new JFileChooser(defaultFileDirectory);
                    chooser.addChoosableFileFilter(SPSUtils.ARCHITECT_FILE_FILTER);
                    chooser.setDialogTitle("Choose Project to Upload");

                    int response = chooser.showOpenDialog(dialog);

                    if (response != JFileChooser.APPROVE_OPTION) {
                        return;
                    }

                    fileField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            });
            builder.add(new JLabel("File"), cc.xy(2, 2));
            builder.add(fileField, cc.xy(4, 2));
            builder.add(fileButton, cc.xy(6, 2));
            
            final JButton okButton = new JButton("Upload");
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    File f = new File(fileField.getText());
                    if (!f.canRead()) {
                        JOptionPane.showMessageDialog(dialogOwner, "File cannot be read", "Invalid File", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    try {
                        ClientSideSessionUtils.uploadProject(getSelectedServerInfo(), nameField.getText(), f, upf, cookieStore);
                        dialog.dispose();
                        refreshInfoList();
                    } catch (Exception ex) {
                        SPSUtils.showExceptionDialogNoReport(dialog, "Unable to upload project", ex);
                    }
                }
            });
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });
            
            okButton.setEnabled(false);
            DocumentListener documentListener = new DocumentListener() {
                @Override
                public void changedUpdate(DocumentEvent e) {
                    textChanged(e);
                }
                @Override
                public void insertUpdate(DocumentEvent e) {
                    textChanged(e);
                }
                @Override
                public void removeUpdate(DocumentEvent e) {
                    textChanged(e);
                }
                private void textChanged(DocumentEvent e) {
                    okButton.setEnabled(!fileField.getText().isEmpty() && !nameField.getText().isEmpty());
                }
            };
            fileField.getDocument().addDocumentListener(documentListener);
            nameField.getDocument().addDocumentListener(documentListener);
            
            JPanel buttonBar = ButtonBarFactory.buildRightAlignedBar(okButton, cancelButton);
            builder.add(buttonBar, cc.xyw(2, 3, 5));
            dialog.add(panel);
            SPSUtils.makeJDialogCancellable(dialog, null);
            dialog.pack();
            dialog.setLocationRelativeTo(dialogOwner);
            dialog.setVisible(true);
        }
    };
    
    private final Action deleteAction = new AbstractAction("Delete") {
        public void actionPerformed(ActionEvent e) {
         
            if (getSelectedServerInfo() != null) {
                
                int [] indices = projects.getSelectedIndices();
                
                if (indices.length >= 1) {
                    
                    final Object [] objs = new Object[indices.length];
                    for (int i = 0; i < indices.length; i++) {
                        objs[i] = projects.getModel().getElementAt(indices[i]);
                    }
                    
                    String promptMessage;
                    if (indices.length == 1) {
                        promptMessage = "Are you sure you want to delete the selected project?" +
                                        "\nThis action cannot be undone.";
                    } else {
                        promptMessage = "Are you sure you want to delete these " + indices.length + " selected projects?" +
                                        "\nThis action cannot be undone.";
                    }
                    
                    if (JOptionPane.showConfirmDialog(dialogOwner, promptMessage, "Confirm Delete Projects", 
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                        for (Object obj : objs) {
                            if (obj instanceof ProjectLocation) {
                                ProjectLocation location = (ProjectLocation) obj;
                                try {
                                    ClientSideSessionUtils.deleteServerWorkspace(location, cookieStore, upf);
                                } catch (Exception ex) {
                                    throw new RuntimeException("Unable to delete project", ex);
                                }
                            } 
                        }
                        
                        refreshInfoList();
                    }
                } 
            }      
        }
    };
    
    private boolean connected = false;
    private SPServerInfo serverInfo = null;
    private final CookieStore cookieStore;
    private final JButton securityButton = new JButton();
    private final JButton openButton = new JButton();

	/**
	 * This constructor creates a dialog for modifying and loading a project
	 * from a single server designated by the given serverInfo parameter.
	 * 
	 * @param serverInfo
	 *            Projects will be retrieved from this server based on the
	 *            information and displayed. The dialog will allow editing the
	 *            security as well as creating and deleting projects on this
	 *            server.
	 * @param dialogOwner
	 *            The dialog to parent new dialogs to.
	 * @param upf
	 *            A user prompter factory for displaying error and warning
	 *            messages to users.
	 * @param closeAction
	 *            An action that will close the dialog or frame that this dialog
	 *            is contained in.
	 * @param defaultFileDirectory
	 *            A default file directory to start looking for files in if the
	 *            user wants to upload a project. If null this will default to
	 *            the user's home directory.
	 * @param cookieStore
	 *            A cookie store for HTTP requests. Used by the
	 *            {@link ClientSideSessionUtils}.
	 */
    public ServerProjectsManagerPanel(
            SPServerInfo serverInfo,
            Component dialogOwner, 
            UserPrompterFactory upf,
            Action closeAction,
            File defaultFileDirectory,
            CookieStore cookieStore) {
        this.serverInfo = serverInfo;
        this.dialogOwner = dialogOwner;
        this.upf = upf;
        this.closeAction = closeAction;
        this.defaultFileDirectory = defaultFileDirectory;
        this.cookieStore = cookieStore;
        
        cookieStore.clear();
        
        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(
                "pref:grow, 5dlu, pref", 
                "pref, pref, pref"));
        
        servers = null;
        
        projects = new JList(new DefaultListModel());
        projects.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                refreshPanel();
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    getOpenAction().actionPerformed(null);
                }
            }
        });
        
        JScrollPane projectsPane = new JScrollPane(projects);
        projectsPane.setPreferredSize(new Dimension(250, 300));
        
        CellConstraints cc = new CellConstraints();    
        builder.add(new JLabel(serverInfo.getName() + "'s projects:"), cc.xyw(1, 1, 2));
        builder.nextLine();
        builder.add(projectsPane, cc.xywh(1, 2, 1, 2));
        
        DefaultFormBuilder buttonBarBuilder = new DefaultFormBuilder(new FormLayout("pref"));      
        buttonBarBuilder.append(new JButton(refreshAction));
        buttonBarBuilder.append(securityButton);
        buttonBarBuilder.append(new JButton(newAction));
        buttonBarBuilder.append(openButton);
        buttonBarBuilder.append(new JButton(deleteAction));
        buttonBarBuilder.append(new JButton(closeAction));
        builder.add(buttonBarBuilder.getPanel(), cc.xy(3, 2));
        builder.setDefaultDialogBorder();
        panel = builder.getPanel();
    }

	/**
	 * Creates a dialog that lets the user choose a server connection and then a
	 * project.
	 * 
	 * @param serverManager
	 *            This object contains all of the server information objects
	 *            known and the servers based on their information will be
	 *            displayed in a list so a user can navigate to different
	 *            projects in different servers.
	 * @param dialogOwner
	 *            The dialog to parent new dialogs to.
	 * @param upf
	 *            A user prompter factory for displaying error and warning
	 *            messages to users.
	 * @param closeAction
	 *            An action that will close the dialog or frame that this dialog
	 *            is contained in.
	 * @param defaultFileDirectory
	 *            A default file directory to start looking for files in if the
	 *            user wants to upload a project. If null this will default to
	 *            the user's home directory.
	 * @param cookieStore
	 *            A cookie store for HTTP requests. Used by the
	 *            {@link ClientSideSessionUtils}.
	 */
    public ServerProjectsManagerPanel(
            SPServerInfoManager serverManager,
            Component dialogOwner, 
            UserPrompterFactory upf,
            Action closeAction,
            File defaultFileDirectory,
            CookieStore cookieStore) {
        this.dialogOwner = dialogOwner;
        this.upf = upf;
        this.closeAction = closeAction;
        this.defaultFileDirectory = defaultFileDirectory;
        this.cookieStore = cookieStore;
        
        cookieStore.clear();
        
        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(
                "pref:grow, 5dlu, pref:grow, 5dlu, pref", 
                "pref, pref, pref"));
        
        servers = new JList(new DefaultListModel());
        servers.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    refreshInfoList();
                }
            }
        });
        
        DefaultListModel serversModel = (DefaultListModel) servers.getModel();
        serversModel.removeAllElements();
        if (serverManager.getServers(false).size() > 0) {
            for (SPServerInfo serverInfo : serverManager.getServers(false)) {
                serversModel.addElement(serverInfo);
            }
        } else {
            serversModel.addElement("No Servers");
            servers.setEnabled(false);
        }
        
        projects = new JList(new DefaultListModel());
        projects.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                refreshPanel();
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    getOpenAction().actionPerformed(null);
                }
            }
        });
        
        JScrollPane projectsPane = new JScrollPane(projects);
        projectsPane.setPreferredSize(new Dimension(250, 300));
        
        JScrollPane serverPane = new JScrollPane(servers);
        serverPane.setPreferredSize(new Dimension(250, 300));
        
        CellConstraints cc = new CellConstraints();    
        builder.add(new JLabel("Servers:"), cc.xyw(1, 1, 2));
        builder.add(new JLabel("Projects:"), cc.xyw(3, 1, 2));
        builder.nextLine();
        builder.add(serverPane, cc.xywh(1, 2, 1, 2));
        builder.add(projectsPane, cc.xywh(3, 2, 1, 2));
        
        DefaultFormBuilder buttonBarBuilder = new DefaultFormBuilder(new FormLayout("pref"));      
        buttonBarBuilder.append(new JButton(refreshAction));
        buttonBarBuilder.append(securityButton);
        buttonBarBuilder.append(new JButton(newAction));
        buttonBarBuilder.append(openButton);
        buttonBarBuilder.append(new JButton(uploadAction));
        buttonBarBuilder.append(new JButton(deleteAction));
        buttonBarBuilder.append(new JButton(closeAction));
        builder.add(buttonBarBuilder.getPanel(), cc.xy(5, 2));
        builder.setDefaultDialogBorder();
        panel = builder.getPanel();
    }
    
    public boolean isConnected() {
        refreshInfoList();
        return connected;
    }
    
    public JPanel getPanel() {
        openButton.setAction(getOpenAction());
        securityButton.setAction(getSecurityAction());
        
        refreshInfoList();
        return panel;
    }
    
    private void refreshPanel() {
        // Update the status of buttons and lists .
        if (connected) {
            
            securityButton.setEnabled(true);
            newAction.setEnabled(true);
            uploadAction.setEnabled(true);
            
            if (projects.isSelectionEmpty()) {
                openButton.setEnabled(false);
                deleteAction.setEnabled(false);
            } else {
                openButton.setEnabled(true);
                deleteAction.setEnabled(true);
            }
            
            projects.setEnabled(true);
        } else {
            securityButton.setEnabled(false);
            newAction.setEnabled(false);
            openButton.setEnabled(false);
            uploadAction.setEnabled(false);
            deleteAction.setEnabled(false);
            projects.setEnabled(false);
        }
    }
    
    protected void refreshInfoList() {
        DefaultListModel model = (DefaultListModel) projects.getModel();
        model.removeAllElements();
        
        SPServerInfo serviceInfo = getSelectedServerInfo();
        if (serviceInfo != null) {
            try {
                List<ProjectLocation> projectLocations = getProjectLocations();
                
                // Sorts the project locations alphabetically
                Collections.sort(projectLocations, new Comparator<ProjectLocation>() {
                    public int compare(ProjectLocation proj1, ProjectLocation proj2) {
                        return proj1.getName().compareToIgnoreCase(proj2.getName());
                    }
                });
                
                model.clear();
                for (ProjectLocation pl : projectLocations) {
                    model.addElement(pl);
                }
                
                connected = true;
            } catch (Exception ex) {
                model.removeAllElements();
                model.addElement("Unable to get projects from server");
                connected = false;
                upf.createUserPrompter("Server Unavailable", 
                        UserPromptType.MESSAGE, 
                        UserPromptOptions.OK, 
                        UserPromptResponse.OK, 
                        "OK", "OK").promptUser("");
            }
            
            refreshPanel();
        } else {
            model.addElement("No Server Selected");
            connected = false;
            refreshPanel();
        }    
    }

    /**
     * Returns all of the project locations for the selected server.
     */
    protected abstract List<ProjectLocation> getProjectLocations() throws 
        IOException, URISyntaxException, JSONException;

    /**
     * Returns the server info for the selected server the user is currently
     * viewing.
     */
    protected SPServerInfo getSelectedServerInfo() {
        if (serverInfo != null) return serverInfo;
        
        int index = servers.getSelectedIndex();
        Object obj;
        
        if (index >= 0) {
            obj = servers.getModel().getElementAt(index);
       
            if (obj instanceof SPServerInfo) {
                return (SPServerInfo) obj;
            } 
        }
        
        return null;
    }
    
    protected Component getDialogOwner() {
        return dialogOwner;
    }
    
    protected Action getCloseAction() {
        return closeAction;
    }
    
    protected List<ProjectLocation> getSelectedProjects() {
        List<ProjectLocation> projectLocations = new ArrayList<ProjectLocation>();
        if (getSelectedServerInfo() != null) {
            
            int [] indices = projects.getSelectedIndices();
            
            if (indices.length >= 1) {
                
                for (int i = 0; i < indices.length; i++) {
                    projectLocations.add((ProjectLocation) projects.getModel().getElementAt(indices[i]));
                }
            }
        }
        return projectLocations;
    }

    /**
     * This action will open the selected project(s).
     */
    protected abstract Action getOpenAction();
    
    /**
     * This action will open a security editor for the selected server.
     */
    protected abstract Action getSecurityAction();
}
