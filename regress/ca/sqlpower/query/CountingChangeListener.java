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

public class CountingChangeListener implements QueryChangeListener {
    
    private int compoundEditEndedCount = 0;
    private int compoundEditStartedCount = 0;
    private int containerAddedCount = 0;
    private int containerRemovedCount = 0;
    private int itemAddedCount = 0;
    private int itemRemovedCount = 0;
    private int itemPropertyChangeEventCount = 0;
    private int joinAddedCount = 0;
    private int joinPropertyChangeEventCount = 0;
    private int joinRemovedCount = 0;
    private int propertyChangeEventCount = 0;
    
    private QueryChangeEvent lastQueryChangeEvent;

    public void compoundEditEnded(TransactionEvent evt) {
        compoundEditEndedCount++;
    }

    public void compoundEditStarted(TransactionEvent evt) {
        compoundEditStartedCount++;
    }

    public void containerAdded(QueryChangeEvent evt) {
        containerAddedCount++;
        lastQueryChangeEvent = evt;
    }

    public void containerRemoved(QueryChangeEvent evt) {
        containerRemovedCount++;
        lastQueryChangeEvent = evt;
    }

    public void itemAdded(QueryChangeEvent evt) {
        itemAddedCount++;
        lastQueryChangeEvent = evt;
    }

    public void itemPropertyChangeEvent(PropertyChangeEvent evt) {
        itemPropertyChangeEventCount++;
    }

    public void itemRemoved(QueryChangeEvent evt) {
        itemRemovedCount++;
        lastQueryChangeEvent = evt;
    }

    public void joinAdded(QueryChangeEvent evt) {
        joinAddedCount++;
        lastQueryChangeEvent = evt;
    }

    public void joinPropertyChangeEvent(PropertyChangeEvent evt) {
        joinPropertyChangeEventCount++;
    }

    public void joinRemoved(QueryChangeEvent evt) {
        joinRemovedCount++;
        lastQueryChangeEvent = evt;
    }

    public void propertyChangeEvent(PropertyChangeEvent evt) {
        propertyChangeEventCount++;
    }

    public int getCompoundEditEndedCount() {
        return compoundEditEndedCount;
    }

    public int getCompoundEditStartedCount() {
        return compoundEditStartedCount;
    }

    public int getContainerAddedCount() {
        return containerAddedCount;
    }

    public int getContainerRemovedCount() {
        return containerRemovedCount;
    }

    public int getItemAddedCount() {
        return itemAddedCount;
    }

    public int getItemRemovedCount() {
        return itemRemovedCount;
    }

    public int getItemPropertyChangeEventCount() {
        return itemPropertyChangeEventCount;
    }

    public int getJoinAddedCount() {
        return joinAddedCount;
    }

    public int getJoinPropertyChangeEventCount() {
        return joinPropertyChangeEventCount;
    }

    public int getJoinRemovedCount() {
        return joinRemovedCount;
    }

    public int getPropertyChangeEventCount() {
        return propertyChangeEventCount;
    }

    public QueryChangeEvent getLastQueryChangeEvent() {
        return lastQueryChangeEvent;
    }

}
