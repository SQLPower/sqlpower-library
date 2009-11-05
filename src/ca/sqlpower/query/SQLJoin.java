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
public class SQLJoin implements Join {
	
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
	private boolean leftColumnOuterJoin;
	
	/**
	 * True if the right column should be an outer join. False otherwise.
	 * If this and isLeftColumnOuterJoin is true then it should be a full
	 * outer join.
	 */
	private boolean rightColumnOuterJoin;
	
	/**
	 * it is one of ">", "<", "=", "<>", ">=", "<=", "BETWEEN", "LIKE", "IN", "NOT".
	 * TODO This should be an enum not a string, especially since its doc comment defines
	 * an enum.
	 */
	private String comparator;
	
	private String name;
	
	private String uuid;
	
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	
	/**
	 * The query this join is in.
	 */
	private Query parent;
	
	public SQLJoin(Item leftColumn, Item rightColumn) {
		this.leftColumn = leftColumn;
		this.rightColumn = rightColumn;
		this.comparator = "=";
		leftColumnOuterJoin = false;
		rightColumnOuterJoin = false;
		uuid = "w" + UUID.randomUUID().toString();
	}

	public Item getLeftColumn() {
		return leftColumn;
	}
	
	public Item getRightColumn() {
		return rightColumn;
	}

	public String getComparator() {
		return comparator;
	}
	
	public void setComparator(String comparator) {
		String oldComparator = this.comparator;
		this.comparator = comparator;
		pcs.firePropertyChange(new PropertyChangeEvent(this, "comparator", oldComparator, comparator));
	}

	public boolean isLeftColumnOuterJoin() {
		return leftColumnOuterJoin;
	}

	public void setLeftColumnOuterJoin(boolean isLeftColumnOuterJoin) {
		boolean oldVal = this.leftColumnOuterJoin;
		this.leftColumnOuterJoin = isLeftColumnOuterJoin;
		
		if (parent != null) {
			parent.startCompoundEdit("Changing join " + name + " for left column outer join to " + this.leftColumnOuterJoin);
		}
		
		pcs.firePropertyChange(new PropertyChangeEvent(this, "leftColumnOuterJoin", 
				oldVal, this.leftColumnOuterJoin));
		
		if (parent != null) {
			parent.endCompoundEdit();
		}
	}
	
	public boolean isRightColumnOuterJoin() {
		return rightColumnOuterJoin;
	}

	public void setRightColumnOuterJoin(boolean isRightColumnOuterJoin) {
		boolean oldVal = this.rightColumnOuterJoin;
		this.rightColumnOuterJoin = isRightColumnOuterJoin;
		
		if (parent != null) {
			parent.startCompoundEdit("Changing join " + name + " for right column outer join to " + this.rightColumnOuterJoin);
		}
		
		pcs.firePropertyChange(new PropertyChangeEvent(this, "rightColumnOuterJoin", 
				oldVal, this.rightColumnOuterJoin));
		
		if (parent != null) {
			parent.endCompoundEdit();
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

	public String getUUID() {
	    return uuid;
	}
	
	public void setUUID(String uuid){
		this.uuid = uuid;
	}
	
	/**
	 * Creates a copy of this join. The new items are the copied items to be used
	 * by the copied join. 
	 */
	public SQLJoin createCopy(Item leftItemCopy, Item rightItemCopy) {
	    SQLJoin join = new SQLJoin(leftItemCopy, rightItemCopy);
	    join.setComparator(getComparator());
	    join.setLeftColumnOuterJoin(isLeftColumnOuterJoin());
	    join.setRightColumnOuterJoin(isRightColumnOuterJoin());
	    join.setName(getName());
	    return join;
	}
	
	public void setParent(Query parent) {
		this.parent = parent;
	}

	public Query getParent() {
		return parent;
	}

}
