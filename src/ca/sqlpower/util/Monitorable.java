/*
 * Copyright (c) 2007, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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