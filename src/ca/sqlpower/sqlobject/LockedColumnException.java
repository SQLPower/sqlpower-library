/*
 * Copyright (c) 2008, SQL Power Group Inc.
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
package ca.sqlpower.sqlobject;


public class LockedColumnException extends RuntimeException {

	private SQLRelationship lockingRelationship;
    private SQLColumn col;

	public LockedColumnException(SQLRelationship lockingRelationship, SQLColumn col) {
		super("Locked column belongs to relationship "+lockingRelationship);
		this.lockingRelationship = lockingRelationship;
        this.col = col;
	}

	public SQLRelationship getLockingRelationship() {
		return lockingRelationship;
	}

    public SQLColumn getCol() {
        return col;
    }

    public void setCol(SQLColumn col) {
        this.col = col;
    }
}
