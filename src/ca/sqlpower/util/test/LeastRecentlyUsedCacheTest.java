package ca.sqlpower.util.test;

import junit.framework.*;
import ca.sqlpower.util.*;

public class LeastRecentlyUsedCacheTest extends CacheTest {

	public void setUp() {
		cache = new LeastRecentlyUsedCache(maxMembers);
		super.setUp();
	}

	public void testLRUPolicy() {
		String val = null;

		// request all items starting with 44
		for (int i = 44; i >= 0; i--) {
			val = (String) cache.get(new Integer(i));
			assertEquals(val, String.valueOf(i));
		}

		// insert 6 new items
		for (int i = 45; i < 51; i++) {
			cache.put(new Integer(i), String.valueOf(i));
		}
		
		// least recently used item should be gone
		assertNull(cache.get(new Integer(44)));
		assertEquals(cache.get(new Integer(43)), String.valueOf(43));

		// insert one more item and re-check (42 will be LRU because we just used 43)
		cache.put(new Integer(52), String.valueOf(52));
		assertNull(cache.get(new Integer(42)));
	}
}
