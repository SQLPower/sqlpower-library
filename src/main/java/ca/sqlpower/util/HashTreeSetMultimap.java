/*
 * Copyright (c) 2012, SQL Power Group Inc.
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

package ca.sqlpower.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.SortedSetMultimap;

/**
 * An implementation of a sorted set multimap that stores the keys in a hash map
 * for constant time look up and values in a tree set.
 * <p>
 * Note that the methods which are documented to allow editing the underlying
 * map directly are not correct for this implementation. If a method is called,
 * like asMap, to get access to the map no modifications will be allowed to the
 * map structure. The objects in the map can still be modified.
 */
public class HashTreeSetMultimap<K, V> implements SortedSetMultimap<K, V> {
	
	private final Map<K, TreeSet<V>> map = new HashMap<K, TreeSet<V>>();
	
	private final Comparator<V> valueComparator;
	
	public HashTreeSetMultimap(Comparator<V> valueComparator) {
		this.valueComparator = valueComparator;
	}

	@Override
	public Set<Entry<K, V>> entries() {
		Set<Entry<K, V>> entries = new HashSet<Entry<K,V>>();
		for (final K k : map.keySet()) {
			for (final V v : map.get(k)) {
				entries.add(new Entry<K, V>() {

					@Override
					public K getKey() {
						return k;
					}

					@Override
					public V getValue() {
						return v;
					}

					@Override
					public V setValue(V value) {
						throw new IllegalStateException();
					}
				});
			}
		}
		return entries;
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public boolean containsEntry(Object k, Object v) {
		return map.get(k).contains(v);
	}

	@Override
	public boolean containsKey(Object k) {
		return map.containsKey(k);
	}

	@Override
	public boolean containsValue(Object v) {
		for (Map.Entry<K, TreeSet<V>> entry : map.entrySet()) {
			if (entry.getValue().contains(v)) return true;
		}
		return false;
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public Multiset<K> keys() {
		return HashMultiset.create(map.keySet());
	}

	@Override
	public boolean put(K k, V v) {
		TreeSet<V> set = map.get(k);
		if (set == null) {
			set = new TreeSet<V>(valueComparator);
			map.put(k, set);
		}
		return set.add(v);
	}

	@Override
	public boolean putAll(Multimap<? extends K, ? extends V> newValues) {
		for (Map.Entry<? extends K, ? extends V> entry : newValues.entries()) {
			put(entry.getKey(), entry.getValue());
		}
		return false;
	}

	@Override
	public boolean putAll(K k, Iterable<? extends V> values) {
		boolean mapChanged = false;
		for (V v : values) {
			mapChanged = mapChanged || put(k, v);
		}
		return mapChanged;
	}

	@Override
	public boolean remove(Object k, Object v) {
		if (map.get(k) == null) return false;
		boolean removed = map.get(k).remove(v);
		if (map.get(k).isEmpty()) {
			map.remove(k);
		}
		return removed;
	}

	@Override
	public int size() {
		return values().size();
	}

	@Override
	public Collection<V> values() {
		List<V> values = new ArrayList<V>();
		for (K k : map.keySet()) {
			values.addAll(map.get(k));
		}
		return null;
	}

	@Override
	public Map<K, Collection<V>> asMap() {
		return Collections.<K, Collection<V>>unmodifiableMap(map);
	}

	@Override
	public SortedSet<V> get(K k) {
		if (map.get(k) == null) return new TreeSet<V>();
		return Collections.unmodifiableSortedSet(map.get(k));
	}

	@Override
	public SortedSet<V> removeAll(Object k) {
		return map.remove(k);
	}

	@Override
	public SortedSet<V> replaceValues(K k, Iterable<? extends V> values) {
		SortedSet<V> replacedValues = new TreeSet<V>();
		for (V v : values) {
			if (remove(k, v)) {
				replacedValues.add(v);
			}
			put(k, v);
		}
		return replacedValues;
	}

	@Override
	public Comparator<? super V> valueComparator() {
		return valueComparator;
	}

}
