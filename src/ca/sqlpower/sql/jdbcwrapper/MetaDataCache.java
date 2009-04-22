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

package ca.sqlpower.sql.jdbcwrapper;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;

import ca.sqlpower.util.Cache;
import ca.sqlpower.util.CacheStats;

/**
 * A cache implementation that holds its values via weak references, so they
 * will be freed when there is memory pressure (or when the cache is flushed,
 * of course).
 * 
 * @param <K> The cache key type
 * @param <V> The cache value type
 */
class MetaDataCache<K, V> implements Cache<K, V> {

    @SuppressWarnings("unchecked")
    private final Map<K, V> data = new ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.SOFT);

    private long lastFlushDate;
    
    private final MyCacheStats stats = new MyCacheStats();
    
    /**
     * A CacheStats type where we can actually increment the values!
     */
    private class MyCacheStats extends CacheStats {
        
        public void incrementHits() {
            totalRequested++;
            totalHits++;
        }
        
        public void incrementMisses() {
            totalRequested++;
            totalMisses++;
        }
        
        public void incrementInserts(int number) {
            totalInserted += number;
        }
        
    };
    
    public MetaDataCache() {
    }

    public void flush() {
        clear();
    }

    public Date getLastFlushDate() {
        return new Date(lastFlushDate);
    }

    public int getMaxMembers() {
        return Integer.MAX_VALUE;
    }

    public CacheStats getStats() {
        return stats;
    }

    public void setMaxMembers(int argMaxMembers) {
        throw new UnsupportedOperationException("This cache uses weak references, and doesn't limit max members");
    }

    public void clear() {
        data.clear();
        stats.cacheFlush();
        lastFlushDate = System.currentTimeMillis();
    }

    public boolean containsKey(Object key) {
        return data.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return data.containsValue(value);
    }

    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return data.entrySet();
    }

    public boolean equals(Object o) {
        return data.equals(o);
    }

    public V get(Object key) {
        V v = data.get(key);
        if (v == null) {
            stats.incrementMisses();
        } else {
            stats.incrementHits();
        }
        return v;
    }

    public int hashCode() {
        return data.hashCode();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public Set<K> keySet() {
        return data.keySet();
    }

    public V put(K key, V value) {
        stats.incrementInserts(1);
        return data.put(key, value);
    }

    public void putAll(Map<? extends K, ? extends V> t) {
        stats.incrementInserts(t.size());
        data.putAll(t);
    }

    public V remove(Object key) {
        return data.remove(key);
    }

    public int size() {
        return data.size();
    }

    public Collection<V> values() {
        return data.values();
    }
    
}
