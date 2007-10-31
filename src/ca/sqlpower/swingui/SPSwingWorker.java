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
package ca.sqlpower.swingui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import ca.sqlpower.swingui.event.TaskTerminationEvent;
import ca.sqlpower.swingui.event.TaskTerminationListener;

public abstract class SPSwingWorker implements Runnable {
	private static final Logger logger = Logger.getLogger(SPSwingWorker.class);
	private Exception doStuffException;
	
	private SPSwingWorker nextProcess;
	private boolean cancelled; 
    private SwingWorkerRegistry registry;
    private Thread thread;
	
    private final List<TaskTerminationListener> taskTerminationListeners
    = new ArrayList<TaskTerminationListener>();

    
    public SPSwingWorker(SwingWorkerRegistry registry) {
        this.registry = registry;
    }
    
	/**
	 * The message that will be displayed in a dialog box if
	 * cleanup() throws an exception. Should be changed by the
	 * subclass calling setCleanupExceptionMessage
	 */
	private String cleanupExceptionMessage = "A problem occurred.";
	
	public final void run() {
		try {
            registry.registerSwingWorker(this);
            thread = Thread.currentThread();
            try {
            	doStuff();
            } catch (Exception e) {
            	doStuffException = e;
            	logger.debug(e.getStackTrace());
            }
            // Do not move into try block above, and too long to be a finally :-)
            SwingUtilities.invokeLater(new Runnable() {
            	public void run() {
            		try {
            			cleanup();
            			fireTaskFinished();
            			
            			if (nextProcess != null) {
            				nextProcess.setCancelled(cancelled);
            				new Thread(nextProcess).start();
            			}
            		} catch (Exception e) {
            			SPSUtils.showExceptionDialogNoReport(cleanupExceptionMessage, e);
            		}
            	}
            });
        } finally {
            registry.removeSwingWorker(this);
            thread = null;
        }
    
	}

	/**
	 * This gets invoked at some time after doStuff() returns.
	 */
	public abstract void cleanup() throws Exception;

	/**
	 * This runs on the thread you provide.  If it throws an exception, you can get it
	 * with getDoStuffException().
	 */
	public abstract void doStuff() throws Exception;
	
	public Exception getDoStuffException() {
		return doStuffException;
	}
    
    public void setDoStuffException(Exception e) {
        doStuffException = e;
    }


	public String getCleanupExceptionMessage() {
		return cleanupExceptionMessage;
	}

	public void setCleanupExceptionMessage(String cleanupExceptionMessage) {
		this.cleanupExceptionMessage = cleanupExceptionMessage;
	}

	public synchronized boolean isCanceled() {
		return cancelled;
	}

	/**
	 * Cancel this and all following tasks
	 * @param cancelled
	 */
	public synchronized void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public SPSwingWorker getNextProcess() {
		return nextProcess;
	}

	public void setNextProcess(SPSwingWorker nextProcess) {
		logger.debug("Moving to object:" + nextProcess);
		this.nextProcess = nextProcess;
	}
	
	/**
	 * Add a TaskTerminationListener that will get notified when the worker is finished 
	 */
	public void addTaskTerminationListener(TaskTerminationListener ttl) {
	    taskTerminationListeners.add(ttl);
	}

	public void removeTaskTerminationListener(TaskTerminationListener ttl) {
	    taskTerminationListeners.remove(ttl);
	}

	private void fireTaskFinished () {
	    TaskTerminationEvent tte = new TaskTerminationEvent(this);
	    for (int i = taskTerminationListeners.size() - 1; i >= 0; i--) {
	        taskTerminationListeners.get(i).taskFinished(tte);
	    }
	}
	
	/**
	 * Sets cancelled to true and interrupts the thread running this SPSwingWorker if it is not null
	 */
	public void kill() {
		if (thread != null) {
			thread.interrupt();
		}
		setCancelled(true);
	}
}
