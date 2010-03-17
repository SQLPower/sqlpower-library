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

public class AbstractSPListener implements SPListener {

    public void childAdded(SPChildEvent e) {
        // stub
    }

    public void childRemoved(SPChildEvent e) {
        // stub
    }

    public void propertyChanged(PropertyChangeEvent evt) {
        // stub
    }

    public void transactionEnded(TransactionEvent e) {
        // stub
    }

    public void transactionRollback(TransactionEvent e) {
        // stub
    }

    public void transactionStarted(TransactionEvent e) {
        // stub
    }

}
