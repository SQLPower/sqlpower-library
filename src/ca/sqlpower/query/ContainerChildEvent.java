/*
 * Copyright (c) 2009, SQL Power Group Inc.
 *
 * This file is part of Wabit.
 *
 * Wabit is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wabit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.query;

/**
 * This event describes when an {@link Item} is added to or removed from a
 * {@link Container}. This event is used in the {@link ContainerChildListener}.
 */
public class ContainerChildEvent {

    /**
     * The source container that had the child item added to or removed from.
     */
    private final Container source;
    
    /**
     * The item that was added to or removed from the container.
     */
    private final Item child;

    /**
     * The location where the child was added to or removed from in the list of
     * items in the container.
     */
    private final int index;
    
    public ContainerChildEvent(Container source, Item child, int index) {
        this.source = source;
        this.child = child;
        this.index = index;
    }

    public Container getSource() {
        return source;
    }

    public Item getChild() {
        return child;
    }

    public int getIndex() {
        return index;
    }
}
