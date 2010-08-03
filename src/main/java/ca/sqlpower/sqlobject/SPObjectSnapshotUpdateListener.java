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

package ca.sqlpower.sqlobject;

import java.beans.PropertyChangeEvent;

import ca.sqlpower.object.SPChildEvent;
import ca.sqlpower.object.SPListener;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.SPObjectSnapshot;
import ca.sqlpower.util.TransactionEvent;

/**
 * An {@link SPListener} that is added to an {@link SPObject}, and is associated
 * with an {@link SPObjectSnapshot}. When the SPObject fires SPListener events,
 * it will notify the snapshot of changes, primarily by setting the obsolete
 * flag on the snapshot to false. It is possible that this listener could also
 * be used to push updates to the snapshots.
 */
public class SPObjectSnapshotUpdateListener implements SPListener {

	private SPObjectSnapshot<? extends SPObject> snapshot;
	
	private boolean setObsolete = false;
	
	private int transactionCount = 0;

	/**
	 * Creates a new {@link SPObjectSnapshotUpdateListener} and associates it
	 * with the given {@link SPObjectSnapshot}.
	 */
	public SPObjectSnapshotUpdateListener(SPObjectSnapshot<? extends SPObject> snapshot) {
		this.snapshot = snapshot;
	}
	
	@Override
	public void childAdded(SPChildEvent e) {
		setObsolete = true;
		e.getChild().addSPListener(this);
	}

	@Override
	public void childRemoved(SPChildEvent e) {
		setObsolete = true;
		e.getChild().removeSPListener(this);
	}

	@Override
	public void transactionStarted(TransactionEvent e) {
		if (transactionCount == 0) {
			setObsolete = false;
		}
		transactionCount++;
	}

	@Override
	public void transactionEnded(TransactionEvent e) {
		transactionCount--;
		if (transactionCount < 0) throw new IllegalStateException("A transaction ended before it began.");
		if (transactionCount == 0 && setObsolete) {
			snapshot.setObsolete(true);
		}
	}

	@Override
	public void transactionRollback(TransactionEvent e) {
		
	}

	@Override
	public void propertyChanged(PropertyChangeEvent evt) {
		setObsolete = true;
		if (transactionCount == 0) {
			snapshot.setObsolete(true);
		}
	}

}
