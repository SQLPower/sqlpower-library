package ca.sqlpower.util;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * An extension of the runnable dispatcher that lets users update the foreground
 * thread. The use of updating the foreground thread is to let a different
 * thread be the foreground for a time instead of the EDT so information can be
 * displayed to the user while the model is being updated.
 */
public interface ExtendedRunnableDispatcher extends RunnableDispatcher {

	/**
	 * Performs the same operation as {@link #runInForeground(Runnable)} except
	 * we get a future that we can call get on if we need to block the thread.
	 * This lets us use the RunnableDispatcher in the same way the EDT uses
	 * invokeAndWait.
	 * 
	 * @param <T>
	 *            The return type of the callable.
	 * @param runner
	 *            The operation to perform.
	 * @return A future that lets us get a response from the runnable, and
	 *         potentially block to wait for the foreground to finish working.
	 */
	<T> Future<T> runInForeground(Callable<T> runner);

	/**
	 * Performs the same operation as {@link #runInBackground(Runnable)} except
	 * we can name the thread to track it.
	 * 
	 * @param runner
	 *            The operation to run on a background thread.
	 * @param name
	 *            The name of the new thread.
	 */
	void runInBackground(final Runnable runner, String name);
}
