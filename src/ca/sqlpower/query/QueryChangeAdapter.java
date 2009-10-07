/*
 * Copyright (c) 2009, SQL Power Group Inc.
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

package ca.sqlpower.query;

import java.beans.PropertyChangeEvent;

import ca.sqlpower.util.TransactionEvent;

/**
 * Classes can extend this adapter to create a {@link QueryChangeListener}
 * if they only want to implement one or two of the methods in the listener.
 */
public class QueryChangeAdapter implements QueryChangeListener {

    public void containerAdded(QueryChangeEvent evt) {
    }

    public void containerRemoved(QueryChangeEvent evt) {
    }

    public void itemAdded(QueryChangeEvent evt) {
    }

    public void itemPropertyChangeEvent(PropertyChangeEvent evt) {
    }

    public void itemRemoved(QueryChangeEvent evt) {
    }

    public void joinAdded(QueryChangeEvent evt) {
    }

    public void joinPropertyChangeEvent(PropertyChangeEvent evt) {
    }

    public void joinRemoved(QueryChangeEvent evt) {
    }

    public void propertyChangeEvent(PropertyChangeEvent evt) {
    }

    public void compoundEditEnded(TransactionEvent evt) {
    }

    public void compoundEditStarted(TransactionEvent evt) {
    }

}
