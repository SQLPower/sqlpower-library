/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package ca.sqlpower.sqlobject;

import java.beans.PropertyChangeEvent;

import ca.sqlpower.object.AbstractPoolingSPListener;
import ca.sqlpower.object.SPChildEvent;
import ca.sqlpower.sqlobject.undo.CompoundEvent;
import ca.sqlpower.util.TransactionEvent;

public class CountingCompoundEventListener extends AbstractPoolingSPListener {

	// ========= Undo Compound Listener ==========
	private int editDepth;
	private int editsInLastGroup;
	private int editsBeforeLastGroup;
	
	@Override
	public void transactionStartedImpl(TransactionEvent e) {
		if (editDepth == 0) {
			editsBeforeLastGroup = liveEdits;
		}
		editDepth++;
	}

	/**
	 * Ends the current compound edit. This method makes sure there was already
	 * a compound edit in progress. If there was not a compound edit in
	 * progress, throws IllegalStateException. In that case, there is either a
	 * bug in the code you're listening to, or you started listening to an
	 * object that was already in the middle of a compound edit.
	 */
	@Override
	public void transactionEndedImpl(TransactionEvent e) {
		if (editDepth == 0) {
			throw new IllegalStateException("Compound edit depth was already 0");
		}
		editDepth--;
		if (editDepth == 0) {
			editsInLastGroup = liveEdits;
			liveEdits = 0;
		}
	}

	/**
	 * Returns how many compound edits have started minus how many
	 * compound edits have finished. Due to fail-fast check on {@link #transactionEnded(CompoundEvent)},
	 * this will never be less than 0.
	 */
	public int getEditDepth() {
		return editDepth;
	}

	/**
	 * Returns the number of edits that happened in the last complete
	 * group of compound edits.  Does not count edits inside the current
	 * compound edit (if there is one).
	 */
	public int getEditsInLastGroup() {
		return editsInLastGroup;
	}
	
	/**
	 * Returns the number of edits that happened outside of any compound
	 * group, before the current compound group started.  If not currently
	 * in a compound group ({@link #getEditDepth()} returns 0), this returns
	 * the count from before the last group started, not the current live edit
	 * count.
	 */
	public int getEditsBeforeLastGroup() {
		return editsBeforeLastGroup;
	}

	// ========= SQL Object Listener ==========
	private int liveEdits;
	
	@Override
	public void childAddedImpl(SPChildEvent e) {
		liveEdits++;
	}

	@Override
	public void childRemovedImpl(SPChildEvent e) {
		liveEdits++;
	}

	@Override
	public void propertyChangeImpl(PropertyChangeEvent e) {
		liveEdits++;
	}

}
