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

package ca.sqlpower.sqlobject;

import java.util.List;

import ca.sqlpower.object.SPObject;

public class SQLCheckConstraint extends SQLObject {

	/*
	 * This class is a stub, put in here so that enterprise tests can work. It
	 * will soon be replaced an actual class.
	 */
	
	
	@Override
	public boolean allowsChildren() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<? extends SQLObject> getChildrenWithoutPopulating() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getShortDisplayName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void populateImpl() throws SQLObjectException {
		// TODO Auto-generated method stub

	}

	@Override
	protected boolean removeChildImpl(SPObject child) {
		// TODO Auto-generated method stub
		return false;
	}

	public int childPositionOffset(Class<? extends SPObject> childType) {
		// TODO Auto-generated method stub
		return 0;
	}

	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<? extends SPObject> getDependencies() {
		// TODO Auto-generated method stub
		return null;
	}

	public void removeDependency(SPObject dependency) {
		// TODO Auto-generated method stub

	}

}
