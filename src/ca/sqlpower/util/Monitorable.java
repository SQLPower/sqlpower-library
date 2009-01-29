/*
 * Copyright (c) 2008, SQL Power Group Inc.
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

import ca.sqlpower.swingui.ProgressWatcher;

/**
 * The Monitorable interface is a generic way for objects which perform certain
 * tasks to make their progress monitorable.  It is usually appropriate for the
 * class that performs the work to implement this interface directly, but there
 * are some cases where many classes share the work of one overall job, and in
 * that case it might be best for them to use a shared instance of
 * {@link MonitorableImpl}.
 *
 * <p>
 * If the interested party is a GUI component, this information can be interpreted
 * by a {@link ProgressWatcher} which will in turn drive a progress bar in the GUI.
 * Other types of user interfaces can provide similar generic classes that use a
 * Monitorable to track progress.
 */
public interface Monitorable {

	/**
	 * Tells how much work has been done.
	 *
	 * @return The amount of work that has been done so far (between 0 and the job size).
	 */
	public int getProgress();

	/**
	 * Tells the size of the job being performed.  If the size is not yet known (because
	 * work needs to be done to calculate the job size), returns null.
	 *
	 * @return An Integer saying how much work must be done; null if this amount is not
	 * yet known.
	 */
	public Integer getJobSize();

	/**
	 * Returns true once the process being monitored has begun.  This will remain true
	 * after {@link #isFinished} becomes true.  The value will only go back to false if
	 * the process being monitored is preparing for a restart (it may go back to false
	 * in this case, but it's not required to, if for instance the process restarts immediately).
	 * 
	 * @return
	 */
	public boolean hasStarted();

	/**
	 * Tells interested parties that the task being performed by this object is finished.
	 * Normally, getJobSize() and getProgress will return equal integers at this point,
	 * but this is not required.  For example, when the user cancels the operation, it will
	 * be finished even though we have not progressed to the end of the job.
	 *
	 * @return True if and only if the process is finished.
	 */
	public boolean isFinished();

	/**
	 * call this to get a message to stick in the dynamic portion of your ProgressMonitor
	 *
	 * @return
	 */
	public String getMessage();

	/**
	 * Lets the ProgressWatcher send a signal to a Monitorable
	 * telling it to cancel itself.
	 *
	 * @param cancelled
	 */
	public void setCancelled(boolean cancelled);

    /**
     * Tells whether or not this monitorable process has received a cancellation
     * request.  If the cancellation request has not yet been honoured, {@link #isFinished()}
     * will be false.  If the cancellation has completed, or the process has completed
     * normally, {@link #isFinished()} will be true. 
     */
    public boolean isCancelled();
    
}