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

import java.util.prefs.Preferences;

import junit.framework.TestCase;
import ca.sqlpower.sql.PlDotIni;

public class PreferencesFactoryTest extends TestCase {
	
	public void testPreReqs() {
		String n = System.getProperty(PreferencesFactory.PREFS_FACTORY_SYSTEM_PROPERTY);
		assertNotNull(MemoryPreferences.SYSTEM_PROPS_ERROR_MESSAGE, n);
		assertEquals(MemoryPreferences.SYSTEM_PROPS_ERROR_MESSAGE, PreferencesFactory.MY_CLASS_NAME, n);		
	}
	
	/*
	 * Test method for 'regress.prefs.PreferencesFactory.systemRoot()'
	 */
	public void testSystemRoot() {
		Object o = null;
		try {
			o = Preferences.systemRoot();
		} catch (Throwable bleah) {
			bleah.printStackTrace();
			return;
		}
		assertNotNull(o);
	}

	/*
	 * Test method for 'regress.prefs.PreferencesFactory.userRoot()'
	 */
	public void testUserRoot() {
		Object o = Preferences.userNodeForPackage(PlDotIni.class);
		assertNotNull(o);
	}
}
