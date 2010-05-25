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

package ca.sqlpower.dao;

/**
 * Persisters of this type are meant to take in persist calls from one persister
 * and upgrade them to a different set of persist calls at a new version of the
 * state of the repository.
 * <p>
 * One important note is that the results of the upgrade should be in-line with what
 * you would get from a JCRPersister if you were loading the project directly.
 */
public interface SPUpgradePersister extends SPPersister {

    /**
     * The next persister will be sent persist calls that this upgrade persister
     * has processed. In some cases the persist calls will just flow to the next
     * persister un-obstructed. In other upgrade persisters all of the persist
     * calls may need to be buffered first then upgraded before sending them to
     * the next persister.
     */
    public void setNextPersister(SPPersister nextPersister);

    /**
     * See {@link #setNextPersister(SPPersister)}.
     * 
     * @return The next persister.
     */
    public SPPersister getNextPersister();
    
    /**
     * Returns the old top level object name of the repository. This may be the
     * same as the new top level object name but it can change. The top level object
     * name is the fully qualified class name of the object that sits at the root
     * of the Architect's project.
     */
    public String getOldTopLevelObjectName();
    
    /**
     * Returns the new top level object name of the repository. This may be the
     * same as the old top level object name but it can change. The top level object
     * name is the fully qualified class name of the object that sits at the root
     * of the Architect's project.
     */
    public String getNewTopLevelObjectName();
}
