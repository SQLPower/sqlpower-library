package ca.sqlpower.sqlobject;

import java.util.LinkedList;

/**
 * This is normally an invisible root node that contains
 * SQLDatabase objects.
 */
public class SQLObjectRoot extends SQLObject {
	public SQLObjectRoot() {
		children = new LinkedList();
	}

	public SQLObject getParent() {
		return null;
	}

	protected void setParent(SQLObject newParent) {
		// no parent
	}

	public String getName() {
		return getShortDisplayName();
	}

	public String getShortDisplayName() {
		return "Database Connections";
	}
	
	public boolean allowsChildren() {
		return true;
	}
	
	public void populateImpl() throws SQLObjectException {
		return;
	}
	
	public boolean isPopulated() {
		return true;
	}

	public String toString() {
		return getShortDisplayName();
	}

	@Override
	public Class<? extends SQLObject> getChildType() {
		return SQLDatabase.class;
	}
}