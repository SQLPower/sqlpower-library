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
}
