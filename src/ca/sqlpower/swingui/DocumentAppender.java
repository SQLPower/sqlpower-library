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

import java.awt.Color;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * A log4j appender which prints logging messages into a Swing document.
 */
public class DocumentAppender extends AppenderSkeleton {

	/**
	 * The document to add logging messages to.
	 */
	private final Document doc;
	
	/**
	 * Boolean to specify whether the document should have
	 * a set number of characters. True, if a limit should
     * be set. 
	 */
	private boolean setLimit;
	
	/**
	 * The maximum number of characters that can be contained 
	 * by the document at any point of time.
	 */
	private int limit;
	
	/**
	 * The visual appearance attributes of the text we put into doc.
	 */
	private final SimpleAttributeSet attributes = new SimpleAttributeSet();
	
	/**
	 * Creates a Log4J appender that writes into the given Swing Document. It
	 * will act like a FIFO buffer where if the boolean setLimit is set to true,
	 * a maximum number of characters that the document can contain is defined
	 * by the integer 'limit'. When the no. of characters in the document
	 * approaches the limit, a few lines from the beginning of the document are
	 * deleted.
	 */
	public DocumentAppender(Document doc, boolean setLimit, int limit) {
		this.doc = doc;
		this.setLimit = setLimit;
		this.limit = limit;
		StyleConstants.setForeground(attributes, Color.BLACK);
		layout = new PatternLayout("%d %p %m\n");
	}
	
	/**
	 * Appends the log message to the target document.
	 */
	@Override
	protected void append(LoggingEvent evt) {
		try {
			doc.insertString(doc.getLength(), layout.format(evt), attributes);
			if (layout.ignoresThrowable()) {
			    String[] throwableStrRep = evt.getThrowableStrRep();
			    if (throwableStrRep != null) {
			        for (String traceElem : throwableStrRep) {
			            doc.insertString(doc.getLength(), traceElem + "\n", attributes);
			        }
			    }
			}
			if (setLimit && doc.getLength() + 25 >= limit) {
				int newlineposition = doc.getText(0, doc.getLength() - 1)
						.indexOf("\n", 25);
				doc.remove(0, newlineposition);
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This is a no-op.
	 */
	public void close() {
		// nothing to do
	}

	/**
	 * I'm not sure if this should return true.
	 */
	public boolean requiresLayout() {
		return true;
	}
	
}