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
 * One important note is that the results of the upgrade should be in-line with
 * what you would get from a JCRPersister if you were loading the project
 * directly.
 * <p>
 * <h2>UUIDs in new objects</h2>
 * <p>
 * Another important note about all upgrades is that adding in a new object
 * requires all of the objects created in the same path to have the same UUID.
 * This means every time a persist call comes through this upgrade path the
 * object created must always have the same UUID, its first child must have the
 * same UUID and so on. The reason for the same UUIDs is old projects in
 * revision control will be updated on-the-fly later and the object defined on
 * that upgrade must match the one created when the workspace is upgraded.
 * <p>
 * The standard pattern for UUIDs to use in an upgrade is
 * "upgrade-#-(generated uuid)-#" where: the first # is the number of the
 * upgrade you are going to put the repository at, the second # is a number that
 * is incremented for each object that is being persisted by that upgrade
 * persister to keep the UUIDs different and the (generated uuid) section is
 * normally a generated UUID that is the same for all objects created by the
 * persister. Provided the objects are created in the same order on every pass
 * of the upgrade persister the UUIDs will be unique for each created object and
 * the same each time the upgrade is run.
 * <p>
 * <h2>Thread Safety</h2>
 * <p>
 * Since the same upgrade persister may be used to upgrade a project multiple
 * times on different threads at the same time some thread safety is required
 * in these types of persisters. For simple upgrade paths this should not be
 * an issue as the upgrade does not need to care about the state of other persist
 * calls. However, if there is some collection of state in the persist calls
 * it should be done in a thread safe way. See {@link ThreadLocal}.
 */
public interface SPUpgradePersister extends SPPersister {

    /**
     * The next persister will be sent persist calls that this upgrade persister
     * has processed. In some cases the persist calls will just flow to the next
     * persister un-obstructed. In other upgrade persisters all of the persist
     * calls may need to be buffered first then upgraded before sending them to
     * the next persister.
     * 
     * @param nextPersister
     *            The next persister to send the upgraded persist calls to.
     * @param isDefault
     *            Set to true if the next persister should be the default
     *            persister used when we access this upgrade persister from a
     *            new thread. The next persister can still be changed.
     */
    public void setNextPersister(SPPersister nextPersister, boolean isDefault);

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
