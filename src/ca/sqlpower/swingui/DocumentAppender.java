/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*MatchMaker.
 *
 * Power*MatchMaker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*MatchMaker is distributed in the hope that it will be useful,
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
	 * The visual appearance attributes of the text we put into doc.
	 */
	private final SimpleAttributeSet attributes = new SimpleAttributeSet();
	
	/**
	 * Creates a Log4J appender that writes into the given Swing Document.
	 */
	public DocumentAppender(Document doc) {
		this.doc = doc;
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