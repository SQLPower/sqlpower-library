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

package ca.sqlpower.sqlobject;

import org.apache.log4j.Logger;

import ca.sqlpower.object.AbstractSPListener;
import ca.sqlpower.object.SPChildEvent;
import ca.sqlpower.util.SQLPowerUtils;


/**
 * This hierarchy listener will add and remove itself to children added to and
 * removed from the object it is listening to. This class will not automatically
 * listen to the hierarchy of the object it is initially placed on,
 * {@link SQLObjectUtils#listenToHierarchy(SQLObjectListener, SQLObject)} can be
 * used to do the initial connecting of this listener to a hierarchy.
 * <p>
 * This class is meant to be extended by listeners that wish to listen to the
 * hierarchy of SQLObjects.
 */
public class SQLObjectHierarchyListener extends AbstractSPListener {
    private static final Logger logger = Logger.getLogger(SQLObjectHierarchyListener.class);

    public void childAddedImpl(SPChildEvent e) {
    	SQLPowerUtils.listenToHierarchy(e.getChild(), this);
    	logger.debug("Adding listener to " + e.getChild() + " of type " + e.getType());
    }

    public void childRemovedImpl(SPChildEvent e) {
    	SQLPowerUtils.unlistenToHierarchy(e.getChild(), this);
    	logger.debug("Removing listener from " + e.getChild());
    }
}
