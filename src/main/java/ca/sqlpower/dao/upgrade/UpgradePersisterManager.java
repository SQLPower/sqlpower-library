/*
 * Copyright (c) 2010, SQL Power Group Inc.
 */

package ca.sqlpower.dao.upgrade;

import ca.sqlpower.dao.SPUpgradePersister;

public interface UpgradePersisterManager {

	/**
	 * The version number of the session. Files exported or saved in this session
	 * will have this version number.
	 */
	public int getStateVersion();
	
	public SPUpgradePersister getUpgradePersister(int version);
	
}
