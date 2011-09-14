/*
 * Copyright (c) 2009, SQL Power Group Inc.
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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Icon;
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

	private static final ImageIcon SEARCH_ICON = new ImageIcon(SearchTextField.class.getClassLoader().getResource("ca/sqlpower/swingui/search.png"));
	private static final ImageIcon REG_EX_ICON = new ImageIcon(SearchTextField.class.getClassLoader().getResource("ca/sqlpower/swingui/searchField_rx.png"));
	private static final ImageIcon EXACT_MATCH_ICON = new ImageIcon(SearchTextField.class.getClassLoader().getResource("ca/sqlpower/swingui/searchField_xm.png"));
	private static final ImageIcon SUBSTRING_ICON = new ImageIcon(SearchTextField.class.getClassLoader().getResource("ca/sqlpower/swingui/searchField_ss.png"));
	private static final ImageIcon DOWN_ARROW_ICON = new ImageIcon(SearchTextField.class.getClassLoader().getResource("ca/sqlpower/swingui/downarrow.png"));
	
	private enum SearchType {
		SUBSTRING,
		REG_EX,
		EXACT_MATCH;
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
	
	/**
	 * This JLabel can be clicked on to display a pop-up to change how the search is
	 * interpreted. The label also contains the icon corresponding to the current method
	 * of search.
	 */
	private JLabel searchTypeIcon;
	
	public SearchTextField(Search s, int columns) {
		this.search = s;
		searchTypePopup = new JPopupMenu();
		searchTypePopup.add(new JMenuItem(new AbstractAction("Substring") {
			public void actionPerformed(ActionEvent e) {
				searchType = SearchType.SUBSTRING;
				searchTypeIcon.setIcon(composeWithArrow(SUBSTRING_ICON));
				doSearch();
			}
		}));
		searchTypePopup.add(new JMenuItem(new AbstractAction("Regular Exp") {
			public void actionPerformed(ActionEvent e) {
				searchType = SearchType.REG_EX;
				searchTypeIcon.setIcon(composeWithArrow(REG_EX_ICON));
				doSearch();
			}
		}));
		searchTypePopup.add(new JMenuItem(new AbstractAction("Exact Match") {
			public void actionPerformed(ActionEvent e) {
				searchType = SearchType.EXACT_MATCH;
				searchTypeIcon.setIcon(composeWithArrow(EXACT_MATCH_ICON));
				doSearch();
			}
		}));
		
		panel = new JPanel();
		searchText = new JTextField(columns);
		searchText.setPreferredSize(new Dimension(searchText.getPreferredSize().width, 20));
		panel.setLayout(new BorderLayout());
		final JLabel searchIconLabel = new JLabel(SEARCH_ICON);
		searchIconLabel.setBackground(searchText.getBackground());
		
		searchTypeIcon = new JLabel(composeWithArrow(SUBSTRING_ICON));
		searchTypeIcon.setBackground(searchText.getBackground());
		searchTypeIcon.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				searchTypePopup.show(panel, searchTypeIcon.getX(), searchTypeIcon.getY() + searchTypeIcon.getHeight());
			}
		});
		
		panel.setBorder(searchText.getBorder());
		panel.setBackground(searchText.getBackground());
		searchText.setBorder(null);
		panel.add(searchIconLabel, BorderLayout.WEST);
		panel.add(searchText);
		panel.add(searchTypeIcon, BorderLayout.EAST);
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
			
		});
	}
	
	private void doSearch() {
		Pattern p;
		boolean matchExactly;
		try {
			switch (searchType) { 
			case REG_EX:
				p = Pattern.compile(getText(), Pattern.CASE_INSENSITIVE);
				matchExactly = true;
				break;
			case SUBSTRING:
				p = Pattern.compile(getText(), Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
				matchExactly = false;
				break;
			case EXACT_MATCH:
				p = Pattern.compile(getText(), Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
				matchExactly = true;
				break;
			default :
				throw new RuntimeException("Searching on the type " + searchType + " has not been implemented.");
			}
		} catch (PatternSyntaxException e) {
			//If the pattern is malformed then search across the empty string.
			p = Pattern.compile("");
			matchExactly = false;
		}
		search.doSearch(p, matchExactly);
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
	
	/**
	 * This will compose a new icon from the base icon and the 
	 * down arrow icon for an icon that will be used to gain access
	 * to a pop-up menu. 
	 */
	private Icon composeWithArrow(Icon baseIcon) {
		return new ComposedIcon(Arrays.asList(new Icon[]{baseIcon, DOWN_ARROW_ICON}));
	}

}
