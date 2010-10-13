/*
 * Copyright (c) 2010, SQL Power Group Inc.
 */

package ca.sqlpower.dao;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;

import ca.sqlpower.dao.SPPersister.DataType;
import ca.sqlpower.dao.upgrade.UpgradePersisterManager;

public class XMLPersisterTest extends PersisterTest {

	ByteArrayOutputStream out = new ByteArrayOutputStream();
	private UpgradePersisterManager upgradePersisterManager;
	
	public void setUp() throws Exception {
		super.setUp();
		upgradePersisterManager = new UpgradePersisterManager() {
			@Override
			public int getStateVersion() {
				return 0;
			}
			
			@Override
			public SPUpgradePersister getUpgradePersister(int version) {
				return null;
			}
		};
		XMLPersister.setUpgradePersisterManager(upgradePersisterManager);
		persister = new XMLPersister(out, "ca.sqlpower.testutil.SPObjectRoot", "tester");
		persister.begin();
		persister.persistObject(null, "ca.sqlpower.testutil.SPObjectRoot", workspaceId, 0);
		persister.persistProperty(workspaceId, "name", DataType.STRING, "rtObjName");
	}
	
	public void testPersistNull() {
		// XMLPersister can't change values, so this test doesn't apply
	}
	
	public void testConditionalPersistProperty() {
		// XMLPersister can't change values, so this test doesn't apply
	}
	
	public void testRemoveObject() {
		// XMLPersister can't change values, so this test doesn't apply
	}
	
	public void testRollback() {
		// XMLPersister can't change values, so this test doesn't apply
		persister.rollback();
		assertEquals("", out.toString());
	}
	
	@Override
	protected void loadWorkspace() throws Exception {
		persister.commit();
		XMLPersisterReader reader = new XMLPersisterReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())), receiver, upgradePersisterManager, "tester");
		reader.read();
	}
	
}
