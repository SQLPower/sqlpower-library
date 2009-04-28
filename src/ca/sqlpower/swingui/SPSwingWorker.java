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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import ca.sqlpower.swingui.event.TaskTerminationEvent;
import ca.sqlpower.swingui.event.TaskTerminationListener;

public abstract class SPSwingWorker implements Runnable {
	private static final Logger logger = Logger.getLogger(SPSwingWorker.class);
	private Throwable doStuffException;
	
	private SPSwingWorker nextProcess;
	private boolean cancelled; 
    private SwingWorkerRegistry registry;
    private Thread thread;
	
    private final List<TaskTerminationListener> taskTerminationListeners
    = new ArrayList<TaskTerminationListener>();
    
	/**
	 * The number of times the timer will fire a property change in a second.
	 * The timer in this worker will start firing events when doStuff starts
	 * and will stop firing events when cleanup is finished.
	 * <p>
	 * This will be null if a timer is not needed.
	 */
	private Integer frequency = null;
	
	/**
	 * The number of ticks that has occurred since the start of the do stuff
	 * method.
	 */
	private int timerTicks;
	
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    
    public SPSwingWorker(SwingWorkerRegistry registry) {
        this.registry = registry;
    }
    
    public SPSwingWorker(SwingWorkerRegistry registry, Integer frequency) {
    	this(registry);
		this.frequency = frequency;
    }
    
	/**
	 * The message that will be displayed in a dialog box if
	 * cleanup() throws an exception. Should be changed by the
	 * subclass calling setCleanupExceptionMessage
	 */
	private String cleanupExceptionMessage = "A problem occurred."; //$NON-NLS-1$
	
	public final void run() {
		try {
			final Timer timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask() {
			
				@Override
				public void run() {
					int oldTicks = timerTicks;
					timerTicks++;
					pcs.firePropertyChange("timerTicks", oldTicks, timerTicks);
				}
			}, 1000/frequency, 1000/frequency);
			
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
            				timer.cancel();
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
	
	/**
	 * Listeners will be notified when the timer ticks. The timer will tick
	 * at the frequency specified in the constructor as ticks/second.
	 */
	public void addTimerListener(PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}
	
	public void removeTimerListener(PropertyChangeListener l) {
		pcs.removePropertyChangeListener(l);
	}
}
