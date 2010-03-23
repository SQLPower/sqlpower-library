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
package ca.sqlpower.swingui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import ca.sqlpower.swingui.event.TaskTerminationEvent;
import ca.sqlpower.swingui.event.TaskTerminationListener;
import ca.sqlpower.util.Monitorable;

public abstract class SPSwingWorker implements Runnable, Monitorable {
	private static final Logger logger = Logger.getLogger(SPSwingWorker.class);
	private Throwable doStuffException;

    private final SwingWorkerRegistry registry;

    /**
     * The core application object that is responsible for this worker. The
     * original purpose for this property was to communicate back to the
     * SwingWorkerRegistry which application object the work is being done for.
     * For example, workers in the Wabit application that execute OLAP queries
     * have this value set to the OlapQuery they're executing (similarly for SQL
     * queries and QueryCache).
     */
    private final Object responsibleObject;
    
	private SPSwingWorker nextProcess;
	private boolean cancelled; 
    private Thread thread;
	
    private final List<TaskTerminationListener> taskTerminationListeners
        = new ArrayList<TaskTerminationListener>();
    
	private boolean started;
	private boolean finished;
	private int progress;
	private String message;
	private Integer jobSize;

    /**
     * Creates a new worker that will register with the given registry when it
     * starts and deregister when it finishes.
     * 
     * @param registry
     *            The registry to notify of task start and completion.
     * @param responsibleObject
     *            The application object that the work is being done for. For
     *            example, workers in the Wabit application would typically
     *            provide a WabitObject and workers in the Architect would
     *            provide a SQLObject.
     *            <p>
     *            This value can be specified as null, which means the work is
     *            not being done on behalf of any particular part of the
     *            application.
     */
    public SPSwingWorker(SwingWorkerRegistry registry, Object responsibleObject) {
        if (registry == null) {
            throw new NullPointerException("Null worker registry is not permitted");
        }
        this.registry = registry;
        this.responsibleObject = responsibleObject;
    }

    public SPSwingWorker(SwingWorkerRegistry registry) {
        this(registry, null);
    }
    
	/**
	 * The message that will be displayed in a dialog box if
	 * cleanup() throws an exception. Should be changed by the
	 * subclass calling setCleanupExceptionMessage
	 */
	private String cleanupExceptionMessage = "A problem occurred."; //$NON-NLS-1$

	public final void run() {
		try {
			setStarted(true);
			setFinished(false);
			
            registry.registerSwingWorker(this);
            thread = Thread.currentThread();
            try {
            	doStuff();
            } catch (Throwable e) {
            	doStuffException = e;
            	logger.debug(e.getStackTrace());
            }
            // Do not move into try block above, and too long to be a finally :-)
            SwingUtilities.invokeLater(new Runnable() {
            	public void run() {
            		try {
            			try {
            				cleanup();
            			} finally {
            				setFinished(true);
            				fireTaskFinished();
            			}
            			
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
	
	public Throwable getDoStuffException() {
		return doStuffException;
	}
    
    public void setDoStuffException(Throwable e) {
        doStuffException = e;
    }


	public String getCleanupExceptionMessage() {
		return cleanupExceptionMessage;
	}

	public void setCleanupExceptionMessage(String cleanupExceptionMessage) {
		this.cleanupExceptionMessage = cleanupExceptionMessage;
	}

	public synchronized boolean isCancelled() {
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
		logger.debug("Moving to object:" + nextProcess); //$NON-NLS-1$
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

	public final synchronized Integer getJobSize() {
		return getJobSizeImpl();
	}
	
	/**
     * Override this method only if something more than using getJobSize
     * and setJobSize is needed. This forces the user to go through
     * a synchronized method for thread safety.
     */
	protected Integer getJobSizeImpl() {
	    return jobSize;
	}
	
	public final synchronized void setJobSize(Integer newJobSize) {
		jobSize = newJobSize;
	}

	public final synchronized String getMessage() {
		return getMessageImpl();
	}
	
	/**
	 * Override this method only if something more than using getMessage
	 * and setMessage is needed. This forces the user to go through
	 * a synchronized method for thread safety.
	 */
	protected String getMessageImpl() {
	    return message;
	}
	
	public final synchronized void setMessage(String newMessage) {
		message = newMessage;
	}

	public final synchronized int getProgress() {
	    return getProgressImpl();
	}
	
	/**
     * Override this method only if something more than using getProgress
     * and setProgress is needed. This forces the user to go through
     * a synchronized method for thread safety.
     */
	protected int getProgressImpl() {
	    return progress;
	}
	
	public final synchronized void setProgress(int newProgress) {
		progress = newProgress;
	}
	
	public final synchronized void increaseProgress() {
	    progress++;
	}

	public final synchronized boolean hasStarted() {
	    return hasStartedImpl();
	}
	
	/**
     * Override this method if something more than using hasStarted
     * and setStarted is needed. This forces the user to go through
     * a synchronized method for thread safety.
     */
	protected boolean hasStartedImpl() {
	    return started;
	}

	public final synchronized boolean isFinished() {
	    return isFinishedImpl();
	}
	
	   /**
     * Override this method only if something more than using isFinished
     * and setFinished is needed. This forces the user to go through
     * a synchronized method for thread safety.
     */
	protected boolean isFinishedImpl() {
	    return finished;
	}
	
	/**
	 * This only needs to be called from outside the SPSwingWorker
	 * if the started flag needs to be set in a place other than at
	 * the start of the doStuff method.
	 */
	protected final synchronized void setStarted(boolean started) {
		this.started= started;
	}
	
	/**
	 * This only needs to be called from outside the SPSwingWorker 
	 * if the finished flag needs to be set in a place other than at
	 * the end of the cleanup method.
	 */
	protected final synchronized void setFinished(boolean finished) {
		this.finished = finished;
	}

    /**
     * Returns the application object this worker is working for, or null if
     * that information is not available.
     * 
     * @return An object in the application's object model, or null.
     */
    public Object getResponsibleObject() {
        return responsibleObject;
    }
}
