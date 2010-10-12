/*
 * Copyright (c) 2010, SQL Power Group Inc.
 */

package ca.sqlpower.dao.upgrade;

import java.util.ArrayList;

import ca.sqlpower.dao.SPUpgradePersister;

public abstract class AbstractUpgradePersisterManager implements
		UpgradePersisterManager {

	private ArrayList<SPUpgradePersister> upgradePersisters = new ArrayList<SPUpgradePersister>();
	
	/**
     * Registers an upgrade persister that can take persist calls from the
     * repository of the form of the old version and convert them to persist
     * calls of the repository at the next version. In order for the upgrade
     * persister to be properly registered there must be an upgrade persister at
     * the previous version or the old version must be the 0th version.
     * 
     * @param oldVersion
     *            The version to upgrade persist calls from. 
     * @param upgradePersister
     *            An upgrade persister that can convert persist calls from a
     *            server at repository version oldVersion to the repository
     *            version of oldVersion + 1.
     * @throws IllegalArgumentException
     *             If the oldVersion is not equal to 0 or there is no persister
     *             currently registered at oldVersion -1 or if a persister is
     *             already registered for this version.
     */
	protected void registerUpgradePersister(int oldVersion, SPUpgradePersister upgradePersister) {
		if (oldVersion < upgradePersisters.size()) throw new IllegalArgumentException(
	            "A persister is already registered to upgrade repositories from version " + oldVersion);
	    if (oldVersion == 0) {
	        upgradePersisters.add(oldVersion, upgradePersister);
	    } else if (oldVersion == upgradePersisters.size()) {
	        upgradePersisters.add(oldVersion, upgradePersister);
	        upgradePersisters.get(oldVersion - 1).setNextPersister(upgradePersister, true);
	    } else {
	        throw new IllegalArgumentException("There is no persister at revision " + 
	                (oldVersion - 1) + " to chain this persister to.");
	    }
	}
	
	@Override
	public SPUpgradePersister getUpgradePersister(int version) {
		if (version >= getStateVersion()) {
			return null;
		} else {
			return upgradePersisters.get(version);
		}
	}

}
