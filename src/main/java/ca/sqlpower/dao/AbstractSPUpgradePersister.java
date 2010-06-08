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

import java.util.HashMap;

/**
 * Override the persister methods in this abstract class 
 * to make easier to read upgrade persisters.
 */
public abstract class AbstractSPUpgradePersister implements SPUpgradePersister {

    /**
     * The default persister to send persists to if there is no next persister
     * defined for the current thread. This allows the upgrade path to be
     * defined once but the ends or temporary intermediate steps to be placed in
     * the upgrade path.
     */
    private SPPersister defaultNextPersister;
    
    /**
     * The persister to forward persist or modified persist events to.
     */
    private ThreadLocal<SPPersister> nextPersister = new ThreadLocal<SPPersister>();

    /**
     * A map of UUIDs to type as they are when
     * {@link #persistObject(String, String, String, int)} is called. This map
     * will only contain the current set of UUIDs for the persist calls so be
     * aware of the order they are sent in if it appears that a UUID is missing.
     */
    private ThreadLocal<HashMap<String, String>> typeMap = new ThreadLocal<HashMap<String, String>>();

    /**
     * The top level object name used for both the
     * {@link #getOldTopLevelObjectName()} and
     * {@link #getNewTopLevelObjectName()} by default.
     */
    private final String topLevelObjectName;

    /**
     * @param topLevelObjectName
     *            Specify the {@link #getOldTopLevelObjectName()} and
     *            {@link #getNewTopLevelObjectName()} here. If the name is to
     *            change in one of the two fields override the method.
     */
    public AbstractSPUpgradePersister(String topLevelObjectName) {
        this.topLevelObjectName = topLevelObjectName;
    }

    public void setNextPersister(SPPersister nextPersister, boolean isDefault) {
        if (isDefault) {
            defaultNextPersister = nextPersister;
        } else {
            this.nextPersister.set(nextPersister);
        }
    }
    
    public SPPersister getNextPersister() {
        if (nextPersister.get() == null) {
            return defaultNextPersister;
        } else {
            return nextPersister.get();
        }
    }

    public void begin() throws SPPersistenceException {
        getNextPersister().begin();
    }

    public void commit() throws SPPersistenceException {
        getNextPersister().commit();
    }

    public void persistObject(String parentUUID, String type, String uuid,
            int index) throws SPPersistenceException {
        HashMap<String, String> localTypeMap = typeMap.get();
        if (localTypeMap == null) {
            localTypeMap = new HashMap<String, String>();
            typeMap.set(localTypeMap);
        }
        localTypeMap.put(uuid, type);
        getNextPersister().persistObject(parentUUID, type, uuid, index);
    }

    public void persistProperty(String uuid, String propertyName,
            DataType propertyType, Object oldValue, Object newValue)
            throws SPPersistenceException {
        getNextPersister().persistProperty(uuid, propertyName, propertyType, oldValue, newValue);
    }

    public void persistProperty(String uuid, String propertyName,
            DataType propertyType, Object newValue)
            throws SPPersistenceException {
        getNextPersister().persistProperty(uuid, propertyName, propertyType, newValue);
    }

    public void removeObject(String parentUUID, String uuid)
            throws SPPersistenceException {
        getNextPersister().removeObject(parentUUID, uuid);
    }

    public void rollback() {
        getNextPersister().rollback();
    }

    public String getNewTopLevelObjectName() {
        return topLevelObjectName;
    }

    public String getOldTopLevelObjectName() {
        return topLevelObjectName;
    }
    
    /**
     * Returns the type of object based on the UUID. This is only 
     * based on the current set of persist calls received. This may
     * return null if the UUID cannot be found. 
     */
    public String getType(String uuid) {
        HashMap<String, String> localTypeMap = typeMap.get();
        if (localTypeMap == null) return null;
        return localTypeMap.get(uuid);
    }
}
