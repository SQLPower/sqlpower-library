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
import java.util.EventObject;

import ca.sqlpower.object.AbstractPoolingSPListener;
import ca.sqlpower.object.SPChildEvent;



/**
 * Helps with testing SQLObject methods that should fire SQLObjectEvents.
 * 
 * @version $Id: CountingSQLObjectListener.java 2909 2009-01-08 20:38:27Z thomasobrien95 $
 */
public class CountingSQLObjectListener extends AbstractPoolingSPListener {
	
	/**
	 * The number of times dbChildredInserted has been called.
	 */
	private int insertedCount;

	/**
	 * The number of times dbChildredRemoved has been called.
	 */
	private int removedCount;
	
	/**
	 * The number of times dbObjectChanged has been called.
	 */
	private int changedCount;
	
	/**
	 * The number of times dbStructureChanged has been called.
	 */
	private int structureChangedCount;
	
	/**
	 * The last SQLObjectEvent that was received by this listener
	 */
	private EventObject lastEvent;
	
	// ============= SQLObjectListener Implementation ==============
	
	/**
	 * Increments the insertedCount.
	 */
	
	@Override
	public void childAddedImpl(SPChildEvent e) {
		lastEvent=e;
		insertedCount++;
	}
	
	/**
	 * Increments the removedCount.
	 */
	@Override
	public void childRemovedImpl(SPChildEvent e) {
		lastEvent=e;
		removedCount++;
	}
	
	/**
	 * Increments the changedCount.
	 */
	@Override
	public void propertyChangeImpl(PropertyChangeEvent e) {
		lastEvent=e;
		changedCount++;
	}
	
	// =========== Getters ============
	
	/**
	 * See {@link #changedCount}.
	 */
	public int getChangedCount() {
		return changedCount;
	}
	
	/**
	 * See {@link #insertedCount}.
	 */
	public int getInsertedCount() {
		return insertedCount;
	}
	
	/**
	 * See {@link #removedCount}.
	 */
	public int getRemovedCount() {
		return removedCount;
	}
	
	/**
	 * See {@link #structureChangedCount}.
	 */
	public int getStructureChangedCount() {
		return structureChangedCount;
	}

	/**
	 * See {@link #lastEvent}
	 */
	public EventObject getLastEvent() {
		return lastEvent;
	}
	
}
