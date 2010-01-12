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

package ca.sqlpower.object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class VariablesTest extends TestCase {

	private MockSPObject root;
	
	private class MockSPObject extends AbstractSPObject implements SPVariableResolverProvider {
		private List<SPObject> children = new ArrayList<SPObject>();
		private SPSimpleVariableResolver resolver;
		public MockSPObject() {
			this.resolver = new SPSimpleVariableResolver(null);
		}
		protected boolean removeChildImpl(SPObject child) {
			return true;
		}
		public boolean allowsChildren() {
			return true;
		}
		public int childPositionOffset(Class<? extends SPObject> childType) {
			return 0;
		}
		public List<? extends SPObject> getChildren() {
			return this.children;
		}
		public List<? extends SPObject> getDependencies() {
			return Collections.emptyList();
		}
		public void removeDependency(SPObject dependency) {
			return;
		}
		public SPSimpleVariableResolver getVariableResolver() {
			return this.resolver;
		}
		@Override
		protected void addChildImpl(SPObject child, int index) {
			this.children.add(child);
		}
		public List<Class<? extends SPObject>> getAllowedChildTypes() {
			List<Class<? extends SPObject>> types = new ArrayList<Class <? extends SPObject>>();
			types.add(SPObject.class);
			return types;
		}
	}
	
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	
	public void testNamespaceHandling() throws Exception {
		
		root = new MockSPObject();
		
		// Test the resolver in namespaced mode
		root.getVariableResolver().setSnobbyResolver(true);
		assertFalse(root.getVariableResolver().resolvesNamespace("namespace"));
		root.getVariableResolver().setNamespace("namespace");
		assertTrue(root.getVariableResolver().resolvesNamespace("namespace"));
		
		// Assigning variables with wrong namespace should fail
		try {
			root.getVariableResolver().store("badnamespace" + SPVariableHelper.NAMESPACE_DELIMITER + "var1", "value1");
			fail("resolver should have rejected this variable because it is not in the right namespace.");
		} catch (Exception e) {
			// as expected...
		}
		
		// Assigning variables with the correct variables should pass.
		root.getVariableResolver().store("namespace" + SPVariableHelper.NAMESPACE_DELIMITER + "var1", "value1");
		root.getVariableResolver().store("namespace" + SPVariableHelper.NAMESPACE_DELIMITER + "var2", "value2");
		
		// Resolving non-namespaced variables while this resolver has one should return null
		assertEquals(null, root.getVariableResolver().resolve("var1"));
		assertEquals(null, root.getVariableResolver().resolve("var2"));
		assertFalse(root.getVariableResolver().resolves("var1"));
		assertFalse(root.getVariableResolver().resolves("var2"));
		assertEquals("value2", root.getVariableResolver().resolve("namespace" + SPVariableHelper.NAMESPACE_DELIMITER + "var2"));
		
		// test with namespace assigned but while in non-snobby mode
		root.getVariableResolver().setSnobbyResolver(false);
		assertTrue(root.getVariableResolver().resolves("var1"));
		assertTrue(root.getVariableResolver().resolves("var2"));
		assertFalse(root.getVariableResolver().resolves("baloneyvar"));
		assertEquals("value1", root.getVariableResolver().resolve("var1"));
		assertEquals("value2", root.getVariableResolver().resolve("var2"));
		
		
		
		// test this resolver without a namespace assigned
		root.getVariableResolver().setNamespace(null);
		assertTrue(root.getVariableResolver().resolvesNamespace("namespace"));
		// In snubby mode, it should ignore namespaced variables
		root.getVariableResolver().setSnobbyResolver(true);
		assertFalse(root.getVariableResolver().resolvesNamespace("namespace"));
		assertEquals(null, root.getVariableResolver().resolve("namespace" + SPVariableHelper.NAMESPACE_DELIMITER + "var1"));
		assertEquals(null, root.getVariableResolver().resolve("namespace" + SPVariableHelper.NAMESPACE_DELIMITER + "var2"));
		// In non-snubby mode, it should resolve namespaced variables
		root.getVariableResolver().setSnobbyResolver(false);
		assertEquals("value1", root.getVariableResolver().resolve("namespace" + SPVariableHelper.NAMESPACE_DELIMITER + "var1"));
		assertEquals("value2", root.getVariableResolver().resolve("namespace" + SPVariableHelper.NAMESPACE_DELIMITER + "var2"));
	}
	
	
	
	public void testParentVariables() throws Exception {
		
		root = new MockSPObject();
		MockSPObject node1 = new MockSPObject();
		MockSPObject node2 = new MockSPObject();
		root.addChild(node1, 0);
		node1.addChild(node2, 0);
		
		root.getVariableResolver().store("key1", "value1");
		
		// If we ask directly each node, only the root node should resolve the variable
		assertFalse(node2.getVariableResolver().resolves("key1"));
		assertFalse(node1.getVariableResolver().resolves("key1"));
		assertTrue(root.getVariableResolver().resolves("key1"));
		
		// If we instanciate a helper and ask it to resolve, it should 
		// be able to resolve it.
		SPVariableHelper helper = new SPVariableHelper(node2);
		assertTrue(helper.resolves("key1"));
		assertEquals("value1", helper.resolve("key1"));
	}
	
	public void testWalkDown() throws Exception {
		
		root = new MockSPObject();
		MockSPObject node1 = new MockSPObject();
		MockSPObject node2 = new MockSPObject();
		MockSPObject node3 = new MockSPObject();
		root.addChild(node1, 0);
		root.addChild(node2, 1);
		node2.addChild(node3, 0);
		
		// Put a variable in a different branch and try to resolve it
		SPVariableHelper helper = new SPVariableHelper(node1);
		node3.getVariableResolver().store("key1", "value1");
		assertFalse(helper.resolves("key1"));
		assertNull(helper.resolve("key1"));
		
		// Now tell the helper to walk back down and resolve it.
		helper.setWalkDown(true);
		assertTrue(helper.resolves("key1"));
		assertEquals("value1", helper.resolve("key1"));
	}
	
	
	public void testResolveCollection() throws Exception {
		
		String[] list1 = new String[2];
		list1[0] = "value1";
		list1[1] = "value2";
		
		String[] list2 = new String[4];
		list2[0] = "value1";
		list2[1] = "value4";
		list2[2] = "value2";
		list2[3] = "value3";
		
		String[] list3 = new String[5];
		list3[0] = "value1";
		list3[1] = "value4";
		list3[2] = "value2";
		list3[3] = "value5";
		list3[4] = "value3";
		
		root = new MockSPObject();
		MockSPObject node1 = new MockSPObject();
		MockSPObject node2 = new MockSPObject();
		MockSPObject node3 = new MockSPObject();
		root.addChild(node1, 0);
		node1.addChild(node2, 0);
		root.addChild(node3, 1);
		
		// Store two values in the root node
		// and 2 in node1
		node1.getVariableResolver().store("key1", "value1");
		node1.getVariableResolver().store("key1", "value2");
		root.getVariableResolver().store("key1", "value3");
		root.getVariableResolver().store("key1", "value4");

		// Bind a helper to node2
		SPVariableHelper helper = new SPVariableHelper(node2);
		
		// resolving them at this point should result in list1
		assertTrue(Arrays.equals(list1, helper.resolveCollection("key1").toArray(new String[2])));
		
		// testing global collection resolve option
		helper.setGlobalCollectionResolve(true);
		assertTrue(Arrays.equals(list2, helper.resolveCollection("key1").toArray(new String[4])));
		
		// Add variables to node 3, which is in another branch and test the walk down
		// with global resolve.
		helper.setWalkDown(true);
		node3.getVariableResolver().store("key1", "value5");
		assertTrue(Arrays.equals(list3, helper.resolveCollection("key1").toArray(new String[5])));
	}
	
	public void testMatches() throws Exception {
		
		String[] list1 = new String[2];
		list1[0] = "foo";
		list1[1] = "fo";
		
		String[] list2 = new String[4];
		list2[0] = "foo";
		list2[1] = "foobar";
		list2[2] = "fooba";
		list2[3] = "foob";
		
		root = new MockSPObject();
		MockSPObject node1 = new MockSPObject();
		MockSPObject node2 = new MockSPObject();
		MockSPObject node3 = new MockSPObject();
		root.addChild(node1, 0);
		node1.addChild(node2, 0);
		root.addChild(node3, 1);
		
		node1.getVariableResolver().store("key1", "fo");
		node1.getVariableResolver().store("key1", "foo");
		root.getVariableResolver().store("key1", "foob");
		root.getVariableResolver().store("key1", "fooba");
		node3.getVariableResolver().store("key3", "foobar");
		node3.getVariableResolver().store("key1", "foobar");
		
		// Bind a helper to node2
		SPVariableHelper helper = new SPVariableHelper(node2);
	
		assertTrue(Arrays.equals(list1, helper.matches("key1", "fo").toArray()));
		assertTrue(Arrays.equals(new String[] {"foo"}, helper.matches("key1", "foo").toArray()));
		assertTrue(Arrays.equals(new String[] {}, helper.matches("key1", "foob").toArray()));
		
		// Now turn walkdown but not globalsearch
		helper.setWalkDown(true);
		assertTrue(Arrays.equals(new String[] {"foobar"}, helper.matches("key3", "foo").toArray()));
		
		// Now global and walkDown
		helper.setGlobalCollectionResolve(true);
		assertTrue(Arrays.equals(list2, helper.matches("key1", "foo").toArray()));
	}
	
	public void testKeySetResolving() throws Exception {
		
		root = new MockSPObject();
		MockSPObject node1 = new MockSPObject();
		MockSPObject node2 = new MockSPObject();
		MockSPObject node3 = new MockSPObject();
		root.addChild(node1, 0);
		node1.addChild(node2, 0);
		root.addChild(node3, 1);
		
		node1.getVariableResolver().store("key1", "foobar");
		root.getVariableResolver().store("key2", "foobar");
		node3.getVariableResolver().store("key3", "foobar");
		
		// Bind a helper to node2
		SPVariableHelper helper = new SPVariableHelper(node2);
		
		// Try fo find keys.
		assertTrue(Arrays.equals(new String[] {"key1", "key2"}, helper.keySet(null).toArray()));
		
		// Now search for keys on the way back too
		helper.setWalkDown(true);
		assertTrue(Arrays.equals(new String[] {"key1", "key2", "key3"}, helper.keySet(null).toArray()));
	}
	
	public void testVariablesDefaultValue() throws Exception {
		
		String defValue = "defValue";
		String keyWithDefValue1 = "key1" + SPVariableResolver.DEFAULT_VALUE_DELIMITER + defValue;
		String keyWithDefValue2 = "namespace" + SPVariableResolver.NAMESPACE_DELIMITER + "key1" + SPVariableResolver.DEFAULT_VALUE_DELIMITER + defValue;
		
		root = new MockSPObject();
		MockSPObject node1 = new MockSPObject();
		MockSPObject node2 = new MockSPObject();
		MockSPObject node3 = new MockSPObject();
		root.addChild(node1, 0);
		root.addChild(node2, 1);
		node2.addChild(node3, 0);
		
		SPVariableHelper helper = new SPVariableHelper(node1);
		helper.setWalkDown(true);
		
		// First test by resolving non-existent variables.
		assertEquals(defValue, helper.resolve(keyWithDefValue1));
		assertEquals(defValue, helper.resolve(keyWithDefValue2));
		assertEquals(1, helper.resolveCollection(keyWithDefValue1).size());
		assertEquals(defValue, helper.resolveCollection(keyWithDefValue1).iterator().next());
		assertEquals(1, helper.resolveCollection(keyWithDefValue2).size());
		assertEquals(defValue, helper.resolveCollection(keyWithDefValue2).iterator().next());
		
		// Set the variables to a different value, without a namespace
		// set.
		node3.getVariableResolver().store("key1", defValue+"X");
		
		// Now resolve again and make sure we get the correct values
		assertEquals(defValue+"X", helper.resolve(keyWithDefValue1));
		assertEquals(defValue, helper.resolve(keyWithDefValue2));
		node3.getVariableResolver().setSnobbyResolver(false);
		assertEquals(defValue+"X", helper.resolve(keyWithDefValue2));
		node3.getVariableResolver().setSnobbyResolver(true);
		assertEquals(1, helper.resolveCollection(keyWithDefValue1).size());
		assertEquals(defValue+"X", helper.resolveCollection(keyWithDefValue1).iterator().next());
		assertEquals(1, helper.resolveCollection(keyWithDefValue2).size());
		assertEquals(defValue, helper.resolveCollection(keyWithDefValue2).iterator().next());
		node3.getVariableResolver().setSnobbyResolver(false);
		assertEquals(1, helper.resolveCollection(keyWithDefValue2).size());
		assertEquals(defValue+"X", helper.resolveCollection(keyWithDefValue2).iterator().next());
		node3.getVariableResolver().setSnobbyResolver(true);
		
		// Set a namespace on the node and do this all over.
		node3.getVariableResolver().setNamespace("namespace");
		node3.getVariableResolver().delete("key1");
		
		// First test by resolving non-existent variables.
		assertEquals(defValue, helper.resolve(keyWithDefValue1));
		assertEquals(defValue, helper.resolve(keyWithDefValue2));
		assertEquals(1, helper.resolveCollection(keyWithDefValue1).size());
		assertEquals(defValue, helper.resolveCollection(keyWithDefValue1).iterator().next());
		assertEquals(1, helper.resolveCollection(keyWithDefValue2).size());
		assertEquals(defValue, helper.resolveCollection(keyWithDefValue2).iterator().next());
		
		// Set the variables to a different value, with a namespace set.
		node3.getVariableResolver().store("namespace" + SPVariableResolver.NAMESPACE_DELIMITER + "key1", defValue+"X");
		
		// Now resolve again and make sure we get the correct values
		assertEquals(defValue, helper.resolve(keyWithDefValue1));
		node3.getVariableResolver().setSnobbyResolver(false);
		assertEquals(defValue+"X", helper.resolve(keyWithDefValue1));
		node3.getVariableResolver().setSnobbyResolver(true);
		assertEquals(defValue+"X", helper.resolve(keyWithDefValue2));
		assertEquals(1, helper.resolveCollection(keyWithDefValue1).size());
		assertEquals(defValue, helper.resolveCollection(keyWithDefValue1).iterator().next());
		node3.getVariableResolver().setSnobbyResolver(false);
		assertEquals(1, helper.resolveCollection(keyWithDefValue1).size());
		assertEquals(defValue+"X", helper.resolveCollection(keyWithDefValue1).iterator().next());
		node3.getVariableResolver().setSnobbyResolver(true);
		assertEquals(1, helper.resolveCollection(keyWithDefValue2).size());
		assertEquals(defValue+"X", helper.resolveCollection(keyWithDefValue2).iterator().next());
	}
}