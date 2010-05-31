/*
 * Copyright (c) 2010, SQL Power Group Inc.
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

import ca.sqlpower.object.AbstractSPObject;
import ca.sqlpower.object.SPObject;

public interface RunnableDispatcher {

    /**
     * This will force the given runnable to execute in the 'foreground'. If
     * something is executed in the foreground then the thread that called this
     * method will pass the runner to the thread that updates the user
     * interface. Once the Runnable has been passed to the UI thread this method
     * will continue executing and the runner will be executed when the UI
     * thread is able to run it.
     * <p>
     * Note: If you are on a background thread and pushing parts of a
     * transaction instead of an entire transaction to the foreground you must
     * be careful about how the foreground is used. If a transaction is sent to
     * the foreground in two or more calls to this method it is possible to have
     * changes come from the foreground of the client or from the server that
     * could change the state of the project. In these cases the parts pushed to
     * the foreground must either be aware of the possibility of these changes
     * or must be done in a way to have the same result regardless of these
     * changes.
     * <p>
     * In cases where there is no UI, the foreground thread will be the same
     * thread as the one calling this method. If this is the case the runner
     * will just have run() called on the same thread. Additionally, if this is
     * called on the foreground thread then it will be run on this thread is
     * they are the same.
     * <p>
     * If you are calling this from a {@link SPObject} that extends
     * {@link AbstractSPObject} you should use the
     * {@link AbstractSPObject#runInForeground(Runnable)} method instead
     * 
     * @param runner
     *            The runnable to run in the foreground.
     */
    void runInForeground(Runnable runner);

	/**
     * This will execute the runnable in a manner that will try to avoid
     * blocking the user interface. This will be done by creating a new thread
     * to execute the Runnable on.
     * <p>
     * In places where there is no UI this runnable will be executed on this
     * thread.
     * <p>
     * If you are calling this from a {@link WabitObject} that extends
     * {@link AbstractWabitObject} you should use the
     * {@link AbstractWabitObject#runInBackground(Runnable)} method instead
     * 
     * 
     * @param runner
     *            The runnable to run in the background.
     */
    void runInBackground(Runnable runner);
    
    /**
	 * Returns true if the current thread is the thread defined as the
	 * foreground thread.
	 * 
	 * @see #runInForeground(Runnable)
	 */
    boolean isForegroundThread();
	
}
