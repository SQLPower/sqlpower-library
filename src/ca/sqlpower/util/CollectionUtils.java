package ca.sqlpower.util;

import java.io.*;
import java.util.*;

/**
 * Provides some handy methods for working with Java Collections.
 */
public class CollectionUtils {

	/**
	 * Given an Iterator, prints the contents of the objects it iterates over.
	 *
	 * @param out The output stream to which the Iterator's set should print.
	 * @param it An Iterator which hopefully <code>hasNext()</code>.
	 */
	public static void printIterator(PrintStream out, Iterator it) {
		out.println("["+it.getClass().getName()+":");
		while(it.hasNext()) {
			out.print("  ");
			out.println(it.next());
		}
		out.println("]");
	}

	/**
	 * Convenience method.  Just casts the given object to Object[]
	 * and calls the other version of this method.
	 */
    public static String arrayToString(Object array) {
		if (array == null) return "null";
		else return arrayToString((Object[]) array);
	}

	/**
	 * Prints each object in the array with commas between them.
	 * Nulls will print as "null".  A null array will cause NPE.
	 */
	public static String arrayToString(Object[] array) {
		StringBuffer outputString = new StringBuffer(100);
		for(int i=0, n=array.length; i<n; i++) {
			if(i>0) outputString.append(", ");
			outputString.append(array[i]);
		}
		return outputString.toString();
	}
}
