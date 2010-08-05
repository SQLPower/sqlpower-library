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
				} else if (!dirty && changed) {
					textChanged();
				}
				changed = dirty;
				dirty = false;
			}
		}
	};

	public TimedDocumentListener(long delay) {
		timer.scheduleAtFixedRate(task, delay, delay);
	}
	
	@Override
	public void changedUpdate(DocumentEvent e) {
		dirty = true;
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		dirty = true;
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		dirty = true;
	}

	/**
	 * Tells the listener to stop forwarding events. If the document has been
	 * changed since the last event was fired, one more event will be fired on
	 * the current thread.
	 */
	public synchronized void cancel() {
		cancelled = true;
		if (dirty || changed) {
			textChanged();
		}
	}

	public abstract void textChanged();

}
