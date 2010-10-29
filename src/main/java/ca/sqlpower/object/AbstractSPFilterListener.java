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

package ca.sqlpower.object;

import java.beans.PropertyChangeEvent;
import java.util.List;

import ca.sqlpower.util.TransactionEvent;

/**
 * This listener will filter the events being fired by examining which of them match
 * the input criteria, in this case, type names. This, combined with the filter
 * persister allow us to skip pieces of the SPObject tree and not load them all.
 */
public class AbstractSPFilterListener implements SPListener {

	/**
	 * A flag that indicates whether we do want to filter out the unwanted
	 * class events.
	 */
	private boolean filter;
	
	/**
	 * A list containing all of the class types we want to filter out from the fired
	 * events.
	 */
	private final List<Class<? extends SPObject>> filteredClasses;
	
	/**
	 * Hols a references to the next listener in the chain so as to allow the hook up.
	 */
	private final SPListener nextListener;
	
	/**
	 * This listener will filter the events being fired by examining which of them match
	 * the input criteria, in this case, type names. 
	 * @param filteredClasses The list of classes with which to filter events
	 * @param nextListener The reference to the next listener in the chain.
	 */
	public AbstractSPFilterListener(List<Class<? extends SPObject>> filteredClasses,
			SPListener nextListener) {
		this.filter = false;
		this.filteredClasses = filteredClasses;
		this.nextListener = nextListener;
		if(filteredClasses.isEmpty()) {
			throw new IllegalArgumentException("You need to specify some classes to filter." +
					"If you don't need the filter, just make an AbstractSPListener.");
		}
	}
	
	@Override
	public void childAdded(SPChildEvent e) {
		if(filter) {
			if(!filteredClasses.contains(e.getChildType())) {
				nextListener.childAdded(e);
			}
		} else {
			nextListener.childAdded(e);
		}
	}

	@Override
	public void childRemoved(SPChildEvent e) {
		if(filter) {
			if(!filteredClasses.contains(e.getChildType())) {
				nextListener.childAdded(e);
			}
		} else {
			nextListener.childRemoved(e);
		}
	}

	@Override
	public void propertyChanged(PropertyChangeEvent evt) {
		if(filter) {
			if(!filteredClasses.contains(evt.getSource().getClass())) {
				nextListener.propertyChanged(evt);
			}
		} else {
			nextListener.propertyChanged(evt);
		}
	}

	@Override
	public void transactionStarted(TransactionEvent e) {
		nextListener.transactionStarted(e);
	}

	@Override
	public void transactionEnded(TransactionEvent e) {
		nextListener.transactionEnded(e);
	}

	@Override
	public void transactionRollback(TransactionEvent e) {
		nextListener.transactionRollback(e);
	}

	public void setFilter(boolean filter) {
		this.filter = filter;
	}

	public boolean isFilter() {
		return filter;
	}

}
