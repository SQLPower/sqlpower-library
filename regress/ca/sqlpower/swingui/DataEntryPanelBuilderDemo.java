/*
 * Copyright (c) 2008, SQL Power Group Inc.
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
package ca.sqlpower.swingui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;

/**
 * Test the ASUtils.createArchitectPanelDialog() method.
 */
public class DataEntryPanelBuilderDemo {
	
	/** For testing the ArchitectPanelBuilder with the default Actions
	 */
	static class TestPanel extends JPanel implements DataEntryPanel {
		TestPanel() {
			setLayout(new BorderLayout());
			add(new JLabel("This is just a test"), BorderLayout.CENTER);
		}

		public boolean applyChanges() {
			System.out.println("You applied your changes");
			return false;
		}

		public void discardChanges() {
			System.out.println("You cancelled your changes");
		}

		public JComponent getPanel() {
			return this;
		}

		public boolean hasUnsavedChanges() {
			return false;
		}
	}

	public static void main(String[] args) {
		
		JFrame frame = new JFrame("Test Main Program");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		final JDialog dlgWithDefaultActions = 
			DataEntryPanelBuilder.createDataEntryPanelDialog(
					new TestPanel(), frame, "Test", "OK Dudes");
		
		Callable<Boolean> okCall = new Callable<Boolean>() {
			public Boolean call() {
				System.out.println("OK action actionPerformed called");
				return new Boolean(true);
			}
		};
		
		Callable<Boolean> cancelCall = new Callable<Boolean>() {
			public Boolean call() {
				System.out.println("cancel action actionPerformed called");
				return new Boolean(true);
			}
		};
		
		final JDialog dlg2 = 
			DataEntryPanelBuilder.createDataEntryPanelDialog(
					new TestPanel(), frame,
					"Test with actions passed in",
					"OK Dudes",
					okCall,
					cancelCall);
		
		frame.add(
			new JLabel("This is the test program's main window",
			JLabel.CENTER),
			BorderLayout.NORTH);	
		
		JButton test1Button = new JButton();
		test1Button.setText("Test Default Actions");
		
		JButton test2Button = new JButton();
		test2Button.setText("Test Caller-Provided Actions");
		
		test1Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dlgWithDefaultActions.setVisible(true);
			}
		});
		
		test2Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dlg2.setVisible(true);
			}
		});
		
		
		JPanel cp = new JPanel(new BorderLayout());
		
		cp.add(ButtonBarFactory.buildOKCancelBar(
				test1Button,
				test2Button),
				BorderLayout.SOUTH);
		cp.setBorder(Borders.DIALOG_BORDER);
		
		frame.add(cp, BorderLayout.SOUTH);

		frame.pack();
		frame.setVisible(true);
	}
}
