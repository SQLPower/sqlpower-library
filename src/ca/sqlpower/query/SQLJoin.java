/*
 * Copyright (c) 2008, SQL Power Group Inc.
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.UUID;


/**
 * A simple SQL object that joins two columns together in a select
 * statement. This will also store how the two columns are being
 * compared. 
 */
public class SQLJoin {
	
	/**
	 * This enum Comparators stores all the comparators.
	 */
	public enum Comparators {
		EQUAL_TO ("="),
	    GREATER_THAN (">"),
	    LESS_THAN   ("<"),
	    GREATER_EQUAL  (">="),
	    LESS_EQUAL    ("<="),
	    NOT_EQUAL ("<>"),
	    BETWEEN  ("BETWEEN"),
	    LIKE  ("LIKE"),
	    NOT  ("NOT"),
	    IN ("IN");
	    
	    private String comparator;

	    Comparators(String op) {
	    	comparator = op;
	    }

	    public String getComparator() {
	        return comparator;
	    }
	}
	
	/**
	 * This property indicates a change to the join in relation to the object
	 * connected by the left part of this join. The left side is not the physical 
	 * side shown in the GUI but the object stored in the leftColumn.
	 */
	public static final String LEFT_JOIN_CHANGED = "LEFT_JOIN_CHANGED";
	
	/**
	 * This property indicates a change to the join in relation to the object
	 * connected by the right part of this join. The right side is not the physical 
	 * side shown in the GUI but the object stored in the rightColumn.
	 */
	public static final String RIGHT_JOIN_CHANGED = "RIGHT_JOIN_CHANGED";
	
	/**
	 * This property indicates a change to the Comparable relation to the object
	 * connected by the left part of this join. 
	 */
	public static final String COMPARATOR_CHANGED = "comparator";
	
	/**
	 * The left column of this join.
	 */
	private final Item leftColumn;

	/**
	 * The right column in the join.
	 */
	private final Item rightColumn;
	
	/**
	 * True if the left column should be an outer join. False otherwise.
	 * If this and isRightColumnOuterJoin is true then it should be a full
	 * outer join.
	 */
	private boolean isLeftColumnOuterJoin;
	
	/**
	 * True if the right column should be an outer join. False otherwise.
	 * If this and isLeftColumnOuterJoin is true then it should be a full
	 * outer join.
	 */
	private boolean isRightColumnOuterJoin;
	
	/**
	 * it is one of ">", "<", "=", "<>", ">=", "<=", "BETWEEN", "LIKE", "IN", "NOT".
	 */
	private String currentComparator;
	
	/**
	 * This is the previous Comparator
	 */
	private String oldComparator;
	
	private String name;
	
	private final UUID uuid;
	
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	public static final String PROPERTY_JOIN_REMOVED = "JOIN_REMOVED";

	public static final String PROPERTY_JOIN_ADDED = "JOIN_ADDED";

	public SQLJoin(Item leftColumn, Item rightColumn) {
		this.leftColumn = leftColumn;
		this.rightColumn = rightColumn;
		this.currentComparator = "=";
		this.oldComparator = "=";
		isLeftColumnOuterJoin = false;
		isRightColumnOuterJoin = false;
		uuid = UUID.randomUUID();
	}

	public Item getLeftColumn() {
		return leftColumn;
	}
	
	public Item getRightColumn() {
		return rightColumn;
	}

	public String getComparator() {
		return currentComparator;
	}
	
	public void setComparator(String currentComp) {
		currentComparator = currentComp;
		pcs.firePropertyChange(new PropertyChangeEvent(this, COMPARATOR_CHANGED, oldComparator, currentComparator));
		oldComparator = currentComp;
	}

	public boolean isLeftColumnOuterJoin() {
		return isLeftColumnOuterJoin;
	}

	public void setLeftColumnOuterJoin(boolean isLeftColumnOuterJoin) {
		if (this.isLeftColumnOuterJoin != isLeftColumnOuterJoin) {
			this.isLeftColumnOuterJoin = isLeftColumnOuterJoin;
			pcs.firePropertyChange(new PropertyChangeEvent(this, LEFT_JOIN_CHANGED, !this.isLeftColumnOuterJoin, this.isLeftColumnOuterJoin));
		}
	}
	
	public boolean isRightColumnOuterJoin() {
		return isRightColumnOuterJoin;
	}

	public void setRightColumnOuterJoin(boolean isRightColumnOuterJoin) {
		if (this.isRightColumnOuterJoin != isRightColumnOuterJoin) {
			this.isRightColumnOuterJoin = isRightColumnOuterJoin;
			pcs.firePropertyChange(new PropertyChangeEvent(this, RIGHT_JOIN_CHANGED, !this.isRightColumnOuterJoin, this.isRightColumnOuterJoin));
		}
	}
	
	public void addJoinChangeListener(PropertyChangeListener l) {
	    pcs.addPropertyChangeListener(l);
	}
	
	public void removeJoinChangeListener(PropertyChangeListener l) {
		pcs.removePropertyChangeListener(l);
	}

    /**
     * This jettisons the current {@link PropertyChangeSupport} object to ensure
     * listeners do not remain attached to the join when it is removed. Used for
     * deleting a join.
     * <p>
     * XXX This should be removed and objects adding listeners to a join should
     * remove the joins appropriately when they are not needed.
     */
	public void removeAllListeners() {
	    pcs = new PropertyChangeSupport(this);
	}
	
	public String getName() {
	    return name;
	}

	public void setName(String name) {
	    String oldName = this.name;
	    this.name = name;
	    pcs.firePropertyChange("name", oldName, name);
	}

	public UUID getUUID() {
	    return uuid;
	}

}
