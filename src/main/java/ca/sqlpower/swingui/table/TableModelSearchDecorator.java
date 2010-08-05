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
package ca.sqlpower.swingui.table;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;

import org.apache.log4j.Logger;
/**
 * Searches through a table model using a table text converter.  It reduces the visible table 
 * rows as rows stop matching.
 * 
 * XXX: This model eats tableChanged events that get thrown from below this should be fixed! 
 */
public class TableModelSearchDecorator extends AbstractTableModel implements CleanupTableModel, TableModelWrapper {

    private static final Logger logger = Logger.getLogger(TableModelSearchDecorator.class);

    /**
     * We need a way of getting the String value of any cell in the table
     * because we need to reliably search for the same text the user sees!
     * The Object.toString() often won't match what the table's cell renderers
     * put on the screen.
     */
    private TableTextConverter tableTextConverter;

    private TableModel tableModel;
    private List<Integer> rowMapping = null;  // null means identity mapping
    private Document doc;
    private String searchText = null;

    /**
     * This is a coalescing timed document listener. It does not support
     * listening to multiple documents. You must instanciate it for each
     * document. You dont need to explicitely add it as it will self
     * register a a document listener.
     */
    private class TimedDocumentListener implements DocumentListener {
    	
    	private AtomicBoolean hasUpdates = new AtomicBoolean(false);
    	
    	private final Timer timer;

		private final Document d;
    	
    	public TimedDocumentListener(Document d) {
    		
    		this.d = d;
    		
    		this.timer = new Timer(500, new ActionListener() {
        		public void actionPerformed(ActionEvent e) {				
    				if (hasUpdates.get()) {
    					hasUpdates.set(false);
    					search(getSearchText(TimedDocumentListener.this.d));
    				}
    			}
    		});

    		d.addDocumentListener(this);
    		
    		this.timer.setInitialDelay(0);
    		this.timer.setCoalesce(true);
    		this.timer.setRepeats(true);
    		this.timer.start();
		}
    	
    	public void cleanup() {
    		this.timer.stop();
    		d.removeDocumentListener(this);
    	}
    	
        private String getSearchText(Document e) {
            String searchText = null;
            try {
                searchText = e.getText(0,e.getLength());
            } catch (BadLocationException e1) {
                throw new RuntimeException(e1);
            }
            return searchText;
        }
        public void insertUpdate(DocumentEvent e) {
            hasUpdates.set(true);
        }

        public void removeUpdate(DocumentEvent e) {
        	hasUpdates.set(true);
        }

        public void changedUpdate(DocumentEvent e) {
        	hasUpdates.set(true);
        }
    };
    
    private TimedDocumentListener docListener = null;
    
    /**
     * This table model listener sends events from the model it wraps up to the parent.
     */
    final TableModelListener tableModelListener = new TableModelListener() {
    	public void tableChanged(TableModelEvent e) {
    	    search(searchText);

    	    // XXX adjust co-ordinates to compensate for missing rows (the ones that don't match the search)
    		fireTableChanged(e);
    	}
    };
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return tableModel.isCellEditable(rowIndex, columnIndex);
    }
    
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
     
       tableModel.setValueAt(aValue, rowIndex, columnIndex);
    }

    public TableModelSearchDecorator(TableModel model) {
        super();
        setWrappedModel(model);
        setDoc(new DefaultStyledDocument());
    }

    private void search(String searchText) {

        rowMapping = null;
        fireTableDataChanged();

        List<Integer> newRowMap = new ArrayList<Integer>();
        String[] searchWords = (searchText == null ? null : searchText.split(" "));
        
        synchronized (tableModel) {
        	for ( int row = 0; row < tableModel.getRowCount(); row++ ) {
        		boolean match = false;
        		if ( searchWords == null ) {
        			match = true;
        		} else {
        			
        			int i;
        			for ( i=0; i<searchWords.length; i++ ) {
        				
        				match = false;
        				for ( int column = 0; column < tableModel.getColumnCount(); column++ ) {
        					Object val = tableModel.getValueAt(row, column);
        					String value = tableTextConverter.getTextForCell(val);
        					if ( value.toLowerCase().indexOf(searchWords[i].toLowerCase()) >= 0 ) {
        						match = true;
        						if (logger.isDebugEnabled()) {
        							logger.debug("Match: "+value.toLowerCase()+" contains "+searchWords[i]+ "     "+value.toLowerCase().indexOf(searchWords[i].toLowerCase()));
        						}
        						break;
        					}
        				}
        				if ( !match )
        					break;
        				
        			}
        			if ( i < searchWords.length )
        				match = false;
        		}
        		if ( match ) {
        			newRowMap.add(row);
        		}
        	}
		}
        
        setSearchText(searchText);
        rowMapping = newRowMap;
        if (logger.isDebugEnabled()) {
        	logger.debug("new row mapping after search: "+rowMapping);
        }
        fireTableDataChanged();
    }

    public int getRowCount() {
        if (rowMapping == null) {
            return tableModel.getRowCount();
        } else {
            return rowMapping.size();
        }
    }

    public int getColumnCount() {
        return tableModel.getColumnCount();
    }


    public Object getValueAt(int rowIndex, int columnIndex) {
        return tableModel.getValueAt(rowToModel(rowIndex),columnIndex);
    }

    private int rowToModel(int rowIndex) {
        int modelRow = (rowMapping == null ? rowIndex : rowMapping.get(rowIndex));
        return modelRow;
    }

    @Override
    public String getColumnName(int column) {
        return tableModel.getColumnName(column);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return tableModel.getColumnClass(columnIndex);
    }


    public TableModel getWrappedModel() {
        return tableModel;
    }

    public void setWrappedModel(TableModel newModel) {
        if (tableModel != null) {
            tableModel.removeTableModelListener(tableModelListener);
        }
        tableModel = newModel;
        newModel.addTableModelListener(tableModelListener);
        fireTableStructureChanged();
    }

    public Document getDoc() {
        return doc;
    }

    public void setDoc(Document doc) {
        if ( this.doc != null
        		&& this.docListener != null) {
            this.docListener.cleanup();
        }

        this.doc = doc;

        if (doc != null) {
            docListener = new TimedDocumentListener(doc);
        }
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public TableTextConverter getTableTextConverter() {
        return tableTextConverter;
    }

    public void setTableTextConverter(TableTextConverter tableTextConverter) {
        this.tableTextConverter = tableTextConverter;
    }

	public void cleanup() {
		docListener.cleanup();
		if (tableModel instanceof CleanupTableModel) {
			((CleanupTableModel) tableModel).cleanup();
		}
	}
}
