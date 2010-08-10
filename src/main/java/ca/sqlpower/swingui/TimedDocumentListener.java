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

package ca.sqlpower.swingui;

import java.util.Timer;
import java.util.TimerTask;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.log4j.Logger;

/**
 * A {@link DocumentListener} that will buffer all events, and throw a new event
 * after the document has not been modified for a specified period. This
 * listener will not forward any specific events. The only notification is a
 * generic change event.
 * 
 * <p>Note that this listener maintains a thread to pass events. You must
 * explicitly {@link #cancel()} this listener for it to be garbage collected.
 */
public abstract class TimedDocumentListener implements DocumentListener {
	
	private static final Logger logger = Logger.getLogger(TimedDocumentListener.class);

	private boolean dirty = false;
	
	private boolean changed = false;

	private Timer timer = new Timer();

	private boolean cancelled = false;
	
	private TimerTask task = new TimerTask() {
		@Override
		public void run() {
			synchronized (TimedDocumentListener.this) {
				if (cancelled) {
					timer.cancel();
					return;
				} else if (!isDirty() && isChanged()) {
					textChanged();
				}
				setChanged(isDirty());
				setDirty(false);
			}
		}
	};

	private final String name;

	public TimedDocumentListener(String name, long delay) {
		this.name = name;
		timer.scheduleAtFixedRate(task, delay, delay);
	}
	
	@Override
	public synchronized void changedUpdate(DocumentEvent e) {
		setDirty(true);
	}

	@Override
	public synchronized void insertUpdate(DocumentEvent e) {
		setDirty(true);
	}

	@Override
	public synchronized void removeUpdate(DocumentEvent e) {
		setDirty(true);
	}
	
	/**
	 * Tells the listener to stop forwarding events. If the document has been
	 * changed since the last event was fired, one more event will be fired on
	 * the current thread.
	 */
	public synchronized void cancel() {
		cancelled = true;
		logger.debug(name + ": cancelled called dirty is " + isDirty() + " changed is " + isChanged());
		if (isDirty() || isChanged()) {
			textChanged();
		}
	}

	public abstract void textChanged();

	private synchronized void setDirty(boolean dirty) {
		this.dirty = dirty;
		logger.debug(name + ": dirty is " + dirty);
	}

	private synchronized boolean isDirty() {
		return dirty;
	}

	private synchronized void setChanged(boolean changed) {
		this.changed = changed;
		logger.debug(name + ": Changed is " + changed);
	}

	private synchronized boolean isChanged() {
		return changed;
	}

}
