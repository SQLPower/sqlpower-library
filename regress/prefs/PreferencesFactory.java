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
package prefs;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import org.apache.log4j.Logger;

/**
 * A java.util.prefs.PreferencesFactory that lets us use the MemoryPreferences
 * so that tests will not affect (nor be affected by) preferences previously created
 * by the user running the tests.
 */
public class PreferencesFactory implements java.util.prefs.PreferencesFactory {

	public static final String PREFS_FACTORY_SYSTEM_PROPERTY = "java.util.prefs.PreferencesFactory";

	public static final String MY_CLASS_NAME = "prefs.PreferencesFactory";

	private static Logger logger = Logger.getLogger(PreferencesFactory.class);

	final static Map<String, Preferences> systemNodes = new HashMap<String, Preferences>();
	
	final Map<String, Preferences> userNodes = new HashMap<String, Preferences>();

	/**
	 * There is always only one System Root node
	 */
	final MemoryPreferences systemRoot = new MemoryPreferences(null, "");

	public Preferences systemRoot() {
		logger.debug("PreferencesFactory.systemRoot()");
		return systemRoot;
	}

	/**
	 * In this implementation there is only one UserRoot, because this
	 * implementation is only used for in-memory testing.
	 */
	final MemoryPreferences userRoot = new MemoryPreferences(null, "");
	
	public Preferences userRoot() {
		logger.debug("PreferencesFactory.userRoot()");
		return userRoot;
	}
	
}
