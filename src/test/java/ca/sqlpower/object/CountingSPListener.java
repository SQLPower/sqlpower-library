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

import ca.sqlpower.util.TransactionEvent;

/**
 * This {@link SPListener} counts the number of child added, child removed,
 * property change, transaction started, transaction ended, and transaction
 * rollback events for use in tests.
 */
public class CountingSPListener implements SPListener {

	protected int childAddedCount = 0;
	protected int childRemovedCount = 0;
	protected int transactionEndedCount = 0;
	protected int transactionRollbackCount = 0;
	protected int transactionStartedCount = 0;
	protected int propertyChangedCount = 0;
	
	
	public void childAdded(SPChildEvent e) {
		childAddedCount++;
	}

	public void childRemoved(SPChildEvent e) {
		childRemovedCount++;
	}

	public void transactionEnded(TransactionEvent e) {
		transactionEndedCount++;
	}

	public void transactionRollback(TransactionEvent e) {
		transactionRollbackCount++;
	}

	public void transactionStarted(TransactionEvent e) {
		transactionStartedCount++;
	}

	public void propertyChanged(PropertyChangeEvent evt) {
		propertyChangedCount++;
	}

	public int getChildAddedCount() {
		return childAddedCount;
	}

	public int getChildRemovedCount() {
		return childRemovedCount;
	}

	public int getTransactionEndedCount() {
		return transactionEndedCount;
	}

	public int getTransactionRollbackCount() {
		return transactionRollbackCount;
	}

	public int getTransactionStartedCount() {
		return transactionStartedCount;
	}

	public int getPropertyChangedCount() {
		return propertyChangedCount;
	}
}
