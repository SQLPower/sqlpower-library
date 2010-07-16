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
			this.resolver = new SPSimpleVariableResolver(this, this.uuid, this.uuid);
		}
		protected boolean removeChildImpl(SPObject child) {
			return true;
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
		assertFalse(root.getVariableResolver().resolvesNamespace("namespace"));
		root.getVariableResolver().setNamespace("namespace");
		assertTrue(root.getVariableResolver().resolvesNamespace("namespace"));
		assertEquals("namespace", root.getVariableResolver().getNamespace());
		
		// Assigning variables should pass.
		root.getVariableResolver().store("var1", "value1");
		root.getVariableResolver().store("var2", "value2");
		
		try {
			root.getVariableResolver().store("baloneyNamespace" + SPVariableResolver.NAMESPACE_DELIMITER + "key", "whatever");
			fail();
		} catch (IllegalArgumentException e) {
			//as expected
		}
		
		assertEquals("value1", root.getVariableResolver().resolve("var1"));
		assertEquals("value2", root.getVariableResolver().resolve("var2"));
		assertTrue(root.getVariableResolver().resolves("var1"));
		assertTrue(root.getVariableResolver().resolves("var2"));
		assertEquals("value1", root.getVariableResolver().resolve("namespace" + SPVariableHelper.NAMESPACE_DELIMITER + "var1"));
		assertEquals("value2", root.getVariableResolver().resolve("namespace" + SPVariableHelper.NAMESPACE_DELIMITER + "var2"));

		assertFalse(root.getVariableResolver().resolves("baloneyvar"));
		
		
		// test this resolver without a namespace assigned
		root.getVariableResolver().setNamespace(null);
		assertFalse(root.getVariableResolver().resolvesNamespace("namespace"));
		assertEquals("myValue", root.getVariableResolver().resolve("namespace" + SPVariableHelper.NAMESPACE_DELIMITER + "var1", "myValue"));
		assertEquals("myValue", root.getVariableResolver().resolve("namespace" + SPVariableHelper.NAMESPACE_DELIMITER + "var2", "myValue"));
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
		
		// If we instanciate a helper bound to the lowest node and ask it to resolve, 
		// a variable at the root, it should be able to resolve it.
		SPVariableHelper helper = new SPVariableHelper(node2);
		assertTrue(helper.resolves("key1"));
		assertEquals("value1", helper.resolve("key1", "defValueReturned"));
	}

	
	
	public void testResolveCollection() throws Exception {
		
		String[] list1 = new String[2];
		list1[0] = "value1";
		list1[1] = "value2";
		
		String[] list2 = new String[4];
		list2[0] = "value1";
		list2[1] = "value2";
		list2[2] = "value3";
		list2[3] = "value4";
		
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
		
		// Add variables to node 3, which is in another branch
		node3.getVariableResolver().store("key1", "value5");
		assertTrue(Arrays.equals(list2, helper.resolveCollection("key1").toArray(new String[4])));
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
		helper.setGlobalCollectionResolve(true);
		assertTrue(Arrays.equals(new String[] {node1.getVariableResolver().getNamespace() + SPVariableResolver.NAMESPACE_DELIMITER + "key1", root.getVariableResolver().getNamespace() + SPVariableResolver.NAMESPACE_DELIMITER + "key2"}, helper.keySet(null).toArray()));
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
		
		SPVariableHelper helper = new SPVariableHelper(node3);
		
		// First test by resolving non-existent variables.
		assertEquals(defValue, helper.resolve(keyWithDefValue1));
		assertEquals(defValue, helper.resolve(keyWithDefValue2));
		assertEquals(1, helper.resolveCollection(keyWithDefValue1).size());
		assertEquals(defValue, helper.resolveCollection(keyWithDefValue1).iterator().next());
		assertEquals(1, helper.resolveCollection(keyWithDefValue2).size());
		assertEquals(defValue, helper.resolveCollection(keyWithDefValue2).iterator().next());
		
		// Set the variables to a different value
		node3.getVariableResolver().update("key1", defValue+"X");
		
		// Now resolve again and make sure we get the correct values
		assertEquals(defValue+"X", helper.resolve(keyWithDefValue1));
		assertEquals(defValue, helper.resolve(keyWithDefValue2));
		assertEquals(defValue, helper.resolve(keyWithDefValue2));
		assertEquals(1, helper.resolveCollection(keyWithDefValue1).size());
		assertEquals(defValue+"X", helper.resolveCollection(keyWithDefValue1).iterator().next());
		assertEquals(1, helper.resolveCollection(keyWithDefValue2).size());
		assertEquals(defValue, helper.resolveCollection(keyWithDefValue2).iterator().next());
		
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
		assertEquals(defValue+"X", helper.resolve(keyWithDefValue1));
		assertEquals(defValue, helper.resolve(keyWithDefValue2));
		assertEquals(1, helper.resolveCollection(keyWithDefValue1).size());
		assertEquals(defValue+"X", helper.resolveCollection(keyWithDefValue1).iterator().next());
		assertEquals(1, helper.resolveCollection(keyWithDefValue2).size());
		assertEquals(defValue, helper.resolveCollection(keyWithDefValue2).iterator().next());
	}
}