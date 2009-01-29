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
package ca.sqlpower.validation.swingui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import ca.sqlpower.validation.DemoTernaryRegExValidator;
import ca.sqlpower.validation.RegExValidator;
import ca.sqlpower.validation.Validator;

import com.jgoodies.forms.builder.ButtonBarBuilder;

/**
 * A complete demonstration of the Validation system,
 */
public class ValidationDemo {

    public static void main(String[] args) {

        final JFrame jf = new JFrame("Demo");
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JButton dialogButton = new JButton("Show dialog");
        jf.add(dialogButton);
        jf.setBounds(200, 200, 200, 200);
        jf.setVisible(true);

        dialogButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final JDialog dialog = new JDialog(jf, "Dialog");
                final StatusComponent display = new StatusComponent();
                final FormValidationHandler validateHandler = new FormValidationHandler(display);
                dialog.add(display, BorderLayout.NORTH);

                JPanel midPanel  = new JPanel(new GridLayout(0, 1 ,5, 5));

                // GUI component references that get used n times
                JPanel p;
                final JTextField tf1;
                JTextField tf;

                // SECTION ONE
                p = new JPanel();
                p.add(new JLabel("Text (\\d+)"));
                tf1 = new JTextField(20);
                p.add(tf1);
                midPanel.add(p);

                // what we came here for #1!!
                Validator v = new RegExValidator("\\d+");
                validateHandler.addValidateObject(tf1,v);

                // SECTION TWO
                p = new JPanel();
                p.add(new JLabel("Text (word)"));
                tf = new JTextField(20);
                p.add(tf);

                midPanel.add(p);

                // what we came here for #2!!
                Validator v2 = new RegExValidator("\\w+", "Must be one word", false);
                validateHandler.addValidateObject(tf,v2);

                // SECTION THREE
                p = new JPanel();
                p.add(new JLabel("OK|WARN|FAIL"));
                tf = new JTextField(20);
                p.add(tf);

                midPanel.add(p);

                // what we came here for #2!!
                Validator v3 = new DemoTernaryRegExValidator();
                validateHandler.addValidateObject(tf,v3);

                dialog.add(midPanel, BorderLayout.CENTER);

                ButtonBarBuilder bPanel = new ButtonBarBuilder();
                bPanel.addGridded(new JButton(new AbstractAction("Insert 123 in numbers"){
                    public void actionPerformed(ActionEvent e) {
                        tf1.setText("123");
                    }}));
                bPanel.addGridded(new JButton(new AbstractAction("Insert abc in numbers"){
                    public void actionPerformed(ActionEvent e) {
                        tf1.setText("abc");
                    }}));

                dialog.add(bPanel.getPanel(), BorderLayout.SOUTH);
                JOptionPane pane;

                dialog.pack();
                dialog.setLocation(200, 200);
                dialog.setVisible(true);
            }

        });

    }

}
