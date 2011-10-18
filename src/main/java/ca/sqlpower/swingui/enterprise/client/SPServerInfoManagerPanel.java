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

package ca.sqlpower.swingui.enterprise.client;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import ca.sqlpower.enterprise.client.ConnectionTestAction;
import ca.sqlpower.enterprise.client.SPServerInfo;
import ca.sqlpower.enterprise.client.SPServerInfoManager;
import ca.sqlpower.swingui.DataEntryPanelBuilder;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A GUI for maintaining the manually-configured server info objects.
 */
public class SPServerInfoManagerPanel {

	private final SPServerInfoManager manager;
	private final Component dialogOwner;
	private final JPanel panel;
	private JList serverInfos;
	private final JButton connectButton;
	private final String boxLable;
	private final String addOrEditDialogLabel;
	
	private ConnectionTestAction testAction = new ConnectionTestAction() {
		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(null, "Testing not implemented");
		}
	};
	
	
	private Action removeAction = new AbstractAction("Remove") {

		public void actionPerformed(ActionEvent e) {
			Object[] selectedValues = serverInfos.getSelectedValues();
			for (Object o : selectedValues) {
				SPServerInfo si = (SPServerInfo) o;
				manager.remove(si);
			}

			refreshInfoList();
		}

	};

	private Action addAction = new AbstractAction("Add...") {
		public void actionPerformed(ActionEvent e) {
			showAddOrEditDialog(null);
		}
	};

	private Action editAction = new AbstractAction("Edit...") {
		public void actionPerformed(ActionEvent e) {
			editSelectedServer();
		}
	};
	
	public SPServerInfoManagerPanel(SPServerInfoManager manager,
			Component dialogOwner, Action closeAction) {
		this(manager, dialogOwner, closeAction, "Available Server Connections:" , "Server Connection Properties");
	}
	
	/**
	 * Creates a panel that displays the currently configured server
	 * connections. New connections can be added from this panel and existing
	 * connections can be modified or removed.
	 * 
	 * @param manager
	 *            An {@link SPServerInfoManager} instance that contains server connection information.
	 * @param dialogOwner
	 *            A component that will be used as the dialog owner for other
	 *            panels.
	 * @param closeAction
	 *            An action that will properly close the object displaying the
	 *            panel.
	 * @param boxLabel
	 *  			Label of the server information box
	 * @param addOrEditDialogLable
	 * 				Label of the Add/Edit panel
	 */
	public SPServerInfoManagerPanel(SPServerInfoManager manager,
			Component dialogOwner, Action closeAction, String boxLabel, String addOrEditDialogLable) {
		this.manager = manager;
		this.dialogOwner = dialogOwner;
		this.boxLable = boxLabel;
		this.addOrEditDialogLabel = addOrEditDialogLable;
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(
				"pref:grow, 5dlu, pref", "pref, pref, pref"));

		serverInfos = new JList(new DefaultListModel());
		serverInfos.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2
						&& SwingUtilities.isLeftMouseButton(e)
						&& connectButton.getAction() != null) {
					connectButton.getAction().actionPerformed(null);
				}
			}

		});
		JScrollPane scrollPane = new JScrollPane(serverInfos);
		scrollPane.setPreferredSize(new Dimension(400, 300));

		this.connectButton = new JButton();
		
		// Build the GUI
		refreshInfoList();
		CellConstraints cc = new CellConstraints();
		builder.add(new JLabel(boxLable), cc
				.xyw(1, 1, 3));
		builder.nextLine();
		builder.add(scrollPane, cc.xywh(1, 2, 1, 2));

		DefaultFormBuilder buttonBarBuilder = new DefaultFormBuilder(
				new FormLayout("pref"));
		buttonBarBuilder.append(new JButton(addAction));
		buttonBarBuilder.append(new JButton(editAction));
		buttonBarBuilder.append(new JButton(removeAction));
		buttonBarBuilder.append(connectButton);
		buttonBarBuilder.append(new JButton(closeAction));
		builder.add(buttonBarBuilder.getPanel(), cc.xy(3, 2));
		builder.setDefaultDialogBorder();
		panel = builder.getPanel();
	}

	private void refreshInfoList() {
		DefaultListModel model = (DefaultListModel) serverInfos.getModel();
		model.removeAllElements();
		for (SPServerInfo si : manager.getServers(false)) {
			model.addElement(si);
		}
	}

	public JPanel getPanel() {
		return panel;
	}

	/**
	 * Shows a dialog for adding a new server info, or editing an existing one.
	 * 
	 * @param serverInfo
	 *            The server info to edit. If the intent is to add a new one,
	 *            specify null.
	 */
	private void showAddOrEditDialog(final SPServerInfo serverInfo) {

		final SPServerInfoPanel infoPanel;
		if (serverInfo == null) {
			infoPanel = new SPServerInfoPanel(panel, manager.getClientVersion(), manager.getDefaultSettings());
		} else {
			infoPanel = new SPServerInfoPanel(panel, manager.getClientVersion(), serverInfo);
		}
		
		testAction.addPanel(infoPanel);
		infoPanel.setTestAction(testAction);

		Callable<Boolean> okCall = new Callable<Boolean>() {
			public Boolean call() throws Exception {
				if (!infoPanel.applyChanges()) {
					return false;
				}
				if (serverInfo != null) {
					manager.remove(serverInfo);
				}
				manager.add(infoPanel.getServerInfo());
				refreshInfoList();
				testAction.removePanel(infoPanel);
				return true;
			}
		};
		Callable<Boolean> cancelCall = new Callable<Boolean>() {
			public Boolean call() throws Exception {
				testAction.removePanel(infoPanel);
				return true;
			}
		};

		JDialog dialog = DataEntryPanelBuilder.createDataEntryPanelDialog(
				infoPanel, dialogOwner, addOrEditDialogLabel, "OK",
				okCall, cancelCall);

		dialog.setVisible(true);
	}
	
	public void setTestAction(ConnectionTestAction action) {
		testAction = action;
	}

	private void editSelectedServer() {
		SPServerInfo selectedItem = getSelectedServer();
		if (selectedItem != null) {
			showAddOrEditDialog(selectedItem);
		}
	}
	
	public SPServerInfo getSelectedServer() {
		return (SPServerInfo) serverInfos.getSelectedValue();
	}
	
	public void setLoginAction(Action action) {
		connectButton.setAction(action);
	}
}
