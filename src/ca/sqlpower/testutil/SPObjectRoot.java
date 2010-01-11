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

package ca.sqlpower.testutil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.sqlpower.object.AbstractSPObject;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.NonProperty;

/**
 * A generic root implementation to contain any type of SPObject implementation.
 */
public class SPObjectRoot extends AbstractSPObject {
	
	private final List<SPObject> children = new ArrayList<SPObject>();

	@Override
	protected boolean removeChildImpl(SPObject child) {
		return children.remove(child);
	}
	
	@Override
	protected void addChildImpl(SPObject child, int index) {
		children.add(index, child);
	}

	public boolean allowsChildren() {
		return true;
	}

	public int childPositionOffset(Class<? extends SPObject> childType) {
		return 0;
	}

	@NonProperty
	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		List<Class<? extends SPObject>> types = new ArrayList<Class<? extends SPObject>>();
		types.add(SPObject.class);
		return types;
	}

	@NonProperty
	public List<? extends SPObject> getChildren() {
		return Collections.unmodifiableList(children);
	}

	@NonProperty
	public List<? extends SPObject> getDependencies() {
		return Collections.emptyList();
	}

	public void removeDependency(SPObject dependency) {
		//no-op
	}

}
