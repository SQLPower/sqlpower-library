/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*MatchMaker.
 *
 * Power*MatchMaker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*MatchMaker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.graph;

import java.util.Comparator;
import java.util.Set;

/**
 * This comparator compares two sets together. Only the first record in each set will actually be
 * compared to each other based on the comparator passed in.
 */
public class FirstElementSetComparator<V extends Object> implements Comparator<Set<V>> {

	Comparator<V> comparator;
	
	public FirstElementSetComparator(Comparator<V> c) {
		comparator = c;
	}
	
	public int compare(Set<V> o1, Set<V> o2) {
		boolean set1Empty = o1.isEmpty() || o1.toArray()[0] == null;
		boolean set2Empty = o2.isEmpty() || o2.toArray()[0] == null;
		
		if (set1Empty && set2Empty) {
			return 0;
		} else if (set1Empty && !set2Empty) {
			return 1;
		} else if (!set1Empty & set2Empty) {
			return -1;
		}
		
		return comparator.compare((V)o1.toArray()[0], (V)o2.toArray()[0]);
	}

}
