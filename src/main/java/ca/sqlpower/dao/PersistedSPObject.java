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

package ca.sqlpower.dao;

import ca.sqlpower.object.SPObject;

/**
 * A class representing an individual persisted {@link SPObject}.
 */
public class PersistedSPObject implements SPTransactionElement, Comparable<PersistedSPObject> {
	private final String parentUUID;
	private final String type;
	private final String uuid;
	/**
	 * The index where the SPObject is stored compared to its siblings in the
	 * parent list. This value is modifiable so the persist object can be
	 * corrected in cases where objects are inserted or removed before it in the
	 * list in the same transaction but the action is done after this persist.
	 * This helps performance instead of removing and adding a new persist call.
	 */
	private int index;

	/**
	 * XXX If set to true this object has been loaded and does not need to
	 * be loaded again. It would be better if this was removed from the
	 * persisted object list but we will have to clean this up later.
	 */
	private boolean loaded = false;

	/**
	 * Constructor to persist a {@link SPObject}.
	 * 
	 * @param parentUUID
	 *            The parent UUID of the {@link SPObject} to persist
	 * @param type
	 *            The {@link SPObject} class name
	 * @param uuid
	 *            The UUID of the {@link SPObject} to persist
	 */
	public PersistedSPObject(String parentUUID, String type,
			String uuid, int index) {
		this.parentUUID = parentUUID;
		this.type = type;
		this.uuid = uuid;
		this.index = index;
	}

	/**
	 * Accessor for the parent UUID field
	 * 
	 * @return The parent UUID of the object to persist
	 */
	public String getParentUUID() {
		return parentUUID;
	}

	/**
	 * Accessor for the {@link SPObject} class name
	 * 
	 * @return The {@link SPObject} class name
	 */
	public String getType() {
		return type;
	}
	
	public String getSimpleType() {
	    String[] s = type.split("\\.");
	    return s[s.length - 1];
	}

	/**
	 * Accessor for the UUID field
	 * 
	 * @return The UUID of the object to persist
	 */
	public String getUUID() {
		return uuid;
	}

	public int getIndex() {
		return index;
	}

	@Override
	public String toString() {
		return "PersistedSPObject: uuid " + uuid + ", parent uuid " + 
				parentUUID + ", type " + type + ", index " + index + 
				", loaded " + loaded;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	public boolean isLoaded() {
		return loaded;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		
		PersistedSPObject pwo = (PersistedSPObject) obj;
		
		if ((getParentUUID() == null && pwo.getParentUUID() != null)
				|| (getParentUUID() != null && !getParentUUID().equals(pwo.getParentUUID())))
			return false;
		
		return getType().equals(pwo.getType()) && getUUID().equals(pwo.getUUID()) 
				&& getIndex() == pwo.getIndex() && isLoaded() == pwo.isLoaded();
		
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		
		result = prime * result + (parentUUID == null ? 0 : parentUUID.hashCode());
		result = prime * result + type.hashCode();
		result = prime * result + uuid.hashCode();
		result = prime * result + index;
		result = prime * result + (loaded ? 1 : 0);
		
		return result;
	}
	
	public int compareTo(PersistedSPObject o) {	 
	    
	    return (getUUID() + getParentUUID()).compareTo(o.getUUID() + o.getParentUUID());
	    
	}

}
