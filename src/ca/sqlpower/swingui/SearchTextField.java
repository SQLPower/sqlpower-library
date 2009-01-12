/*
 * Copyright (c) 2009, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ca.sqlpower.swingui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * This text field is specifically for searching. It has a search
 * icon on it that can be clicked to change the way the searching
 * is done.
 */
public class SearchTextField {

	private static final ImageIcon searchIcon = new ImageIcon(SearchTextField.class.getClassLoader().getResource("ca/sqlpower/swingui/search.png"));
	
	private enum SearchType {
		SUBSTRING,
		REG_EX;
	}
	
	private final JPanel panel;
	
	/**
	 * This is the text field that users enter the actual text to search on.
	 */
	private final JTextField searchText;

	/**
	 * This pop-up lets the user select the type of search they want to use. For
	 * example they can search with plain text or regular expressions.
	 */
	private final JPopupMenu searchTypePopup;
	
	private SearchType searchType = SearchType.SUBSTRING;

	/**
	 * The search object that doSearch will be called on every time the search text
	 * changes.
	 */
	private final Search search;
	
	public SearchTextField(Search s, int columns) {
		this.search = s;
		searchTypePopup = new JPopupMenu();
		searchTypePopup.add(new JMenuItem(new AbstractAction("Regular Exp") {
			public void actionPerformed(ActionEvent e) {
				searchType = SearchType.REG_EX;
			}
		}));
		searchTypePopup.add(new JMenuItem(new AbstractAction("Substring") {
			public void actionPerformed(ActionEvent e) {
				searchType = SearchType.SUBSTRING;
			}
		}));
		
		panel = new JPanel();
		searchText = new JTextField(columns);
		panel.setLayout(new BorderLayout());
		final JLabel iconLabel = new JLabel(searchIcon);
		iconLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				searchTypePopup.show(panel, iconLabel.getX(), iconLabel.getY() + iconLabel.getHeight());
			}
		});
		iconLabel.setBackground(searchText.getBackground());
		panel.setBorder(searchText.getBorder());
		panel.setBackground(searchText.getBackground());
		searchText.setBorder(null);
		panel.add(iconLabel, BorderLayout.WEST);
		panel.add(searchText);
		searchText.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
				doSearch();
			}
			public void insertUpdate(DocumentEvent e) {
				doSearch();
			}
			public void changedUpdate(DocumentEvent e) {
				doSearch();
			}
			
			private void doSearch() {
				Pattern p;
				try {
					switch (searchType) { 
					case REG_EX:
						p = Pattern.compile(getText());
						break;
					case SUBSTRING:
						p = Pattern.compile(getText(), Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
						break;
					default :
						throw new RuntimeException("Searching on the type " + searchType + " has not been implemented.");
					}
				} catch (PatternSyntaxException e) {
					//If the pattern is malformed then search across the empty string.
					p = Pattern.compile("");
				}
				search.doSearch(p);
			}
		});
	}
	
	public JPanel getPanel() {
		return panel;
	}
	
	public JTextField getTextField() {
		return searchText;
	}
	
	public String getText() {
		return searchText.getText();
	}
	
	public void setText(String text) {
		searchText.setText(text);
	}

	public void clear() {
		searchText.setText("");
	}
	
}
