/*
 * Copyright (c) 2010, SQL Power Group Inc.
 */

package ca.sqlpower.enterprise.jcr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.sqlpower.dao.PersistedSPOProperty;
import ca.sqlpower.dao.PersistedSPObject;
import ca.sqlpower.dao.SPPersistenceException;
import ca.sqlpower.dao.SPPersister;


// TODO: This class is copied from SQLPower-enterprise-library and needs to be removed from there 
// and then the related products need to be modified accordingly. 

/**
 * This persister pools all of the persists in one transaction and allows access
 * to them through getters. The lists will not be cleared until the clear method 
 * is called.
 * <p>
 * TODO Make the Differ class use this pooling persister instead of the one
 * built into it. Not doing this now as there's already a large group of changes
 * getting built into this commit. There are likely other places, possibly in tests
 * where a similar type of persister exists but I cannot seem to find one.
 */
public class PoolingPersisterEnterprise implements SPPersister {

    /**
     * Stores all of the persist object calls received since this object was
     * created or the last call to clear.
     */
    private final List<PersistedSPObject> spObjectList = new ArrayList<PersistedSPObject>();
    /**
     * Stores all of the persist property calls received since this object was
     * created or the last call to clear. 
     */
    private final List<PersistedSPOProperty> spoPropertyList = new ArrayList<PersistedSPOProperty>();
    
    /**
     * Stores all of the remove object calls received since this object was
     * created or the last call to clear. Each call is represented by a mapping
     * of the child's UUID to the parent's UUID of the child that was removed.
     */
    private Map<String, String> spObjectMapToRemove = new HashMap<String, String>();

    public void begin() {
        // don't need to do anything
    }
    
    public void commit() {
        // don't need to do anything
    }

    public void persistObject(String parentUUID, String type, String uuid,
            int index) throws SPPersistenceException {
        
        spObjectList.add(new PersistedSPObject(parentUUID, type, uuid, index));
        
    }

    public void persistProperty(String uuid, String propertyName,
            DataType propertyType, Object oldValue, Object newValue)
            throws SPPersistenceException {
        
        spoPropertyList.add(new PersistedSPOProperty(uuid, propertyName, 
                propertyType, oldValue, newValue, false));
        
    }

    public void persistProperty(String uuid, String propertyName,
            DataType propertyType, Object newValue)
            throws SPPersistenceException {
        
        spoPropertyList.add(new PersistedSPOProperty(uuid, propertyName, 
                propertyType, newValue, newValue, true));
        
    }

    public void removeObject(String parentUUID, String uuid)
            throws SPPersistenceException {
        
        spObjectMapToRemove.put(uuid, parentUUID);
        
    }

    public void rollback() {
        throw new IllegalStateException("JCR Persistor rolled back when creating revision.");
    }
    
    /**
     * Returns the list of create SPObject calls.
     * @return
     */
    public List<PersistedSPObject> getSpObjectList() {
        return Collections.unmodifiableList(spObjectList);
    }
    
    /**
     * Returns the list of persist property calls.
     * @return
     */
    public List<PersistedSPOProperty> getSpoPropertyList() {
        return Collections.unmodifiableList(spoPropertyList);
    }
    
    /**
     * Returns the mapping of remove object calls where each entry maps
     * the UUID of the child to be removed to the parent UUID.
     */
    public Map<String, String> getSpObjectMapToRemove() {
        return Collections.unmodifiableMap(spObjectMapToRemove);
    }
    
    /**
     * Clears the persisted objects and properties lists.
     */
    public void clear() {
        spoPropertyList.clear();
        spObjectList.clear();
        spObjectMapToRemove.clear();
    }
}
