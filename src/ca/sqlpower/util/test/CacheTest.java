package ca.sqlpower.util.test;

import junit.framework.*;
import ca.sqlpower.util.*;

/**
 * Tests conformance to the Cache interface.  Must be subclassed and
 * used as part of the test for a concrete Cache implementation.
 */
public abstract class CacheTest extends TestCase {

	public static int maxMembers = 50;

	/**
	 * This is the cache instance we're testing.
	 */
	public Cache cache;

	/**
	 * Puts 45 things into the cache.  It must have been created by
	 * the subclass's setUp method before this method is invoked.  The
	 * things will be keys of type Integer (values 0..45) mapping to
	 * values of type String (string representations of the integer
	 * values of those keys).
	 */
	public void setUp() {
		if (cache == null) fail("Your setUp method must create a new Cache and assign it to this.cache before calling super.setUp().");
		for (int i = 0; i < 45; i++) {
			cache.put(new Integer(i), String.valueOf(i));
		}
	}

	/**
	 * Tests that maxMembers is enforced as a hard limit.  This is
	 * expected but not required bythe Cache interface, so if you're
	 * writing a test for a cache that uses soft limits, override this
	 * test!
	 */
	public void testMaxMembersHardLimitOnInsert() {
		for (int i = 45; i < 60; i++) {
			cache.put(new Integer(i), String.valueOf(i));
		}
		assertTrue(cache.size() <= cache.getMaxMembers());
	}

	public void testFlush() {
		cache.flush();
		assertEquals(0, cache.size());
	}
	
	public void testSize() {
		assertEquals(45, cache.size());
		cache.put(new Integer(45), String.valueOf(45));
		assertEquals(46, cache.size());
	}
}
