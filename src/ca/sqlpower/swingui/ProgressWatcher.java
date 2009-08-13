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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.ProgressMonitor;
import javax.swing.Timer;

import org.apache.log4j.Logger;

import ca.sqlpower.util.Monitorable;

/**
 * This class updates a JProgressBar based on the progress of a given Monitorable
 * object. It places itself as the ActionListener within a Timer object, and uses
 * it to periodically update the progress bar. The ProgressWatcher also allows you to
 * set whether or not 
 */
public class ProgressWatcher implements ActionListener {
    private JProgressBar bar = null;
    private boolean hideProgressBarWhenFinished = false;
    private ProgressMonitor pm = null;
    private Monitorable monitorable = null;
    private JLabel label = null;
    private boolean hideLabelWhenFinished = false;
    private Timer timer;

    private static final Logger logger = Logger.getLogger(ProgressWatcher.class);

    /**
     * Create a ProgressWatcher with the given progress bar and Monitorable.
     * It will leave the progress bar label blank. The label and progress bar will 
     * remain visible after the monitorable is finished by default.  
     * <p>
     * The progress bar will not start up at this point. You should either use the static method
     * {@link #watchProgress(JProgressBar, Monitorable)} or call the {@link #start()}
     * method after calling the constructor to start the progress bar monitor.
     * 
     * @param bar The JProgressBar that is tracking the progress of the monitorable
     * @param monitorable The monitorable object that the ProgressWatcher monitors 
     */
    public ProgressWatcher(JProgressBar bar, Monitorable monitorable) {
        this (bar,monitorable,null);
    }

    /**
     * Create a ProgressWatcher with the given progress bar and Monitorable, and
     * set the progress bar label to the given JLabel. The label and progress
     * bar will remain visible after the monitorable is finished by default.
     * <p>
     * The progress bar will not start up at this point. You should either use
     * the static method
     * {@link #watchProgress(JProgressBar, Monitorable, JLabel))} instead of
     * invoking this constructor directly, or call the {@link #start()} method
     * after calling the constructor to start the progress bar monitor.
     * 
     * @param bar
     *            The JProgressBar that is tracking the progress of the
     *            monitorable
     * @param monitorable
     *            The monitorable object that the ProgressWatcher monitors
     * @param label
     *            The label you want to display on the progress bar
     */
    public ProgressWatcher(JProgressBar bar, Monitorable monitorable, JLabel label) {
        this.bar = bar;
        this.monitorable = monitorable;
        this.label = label;
    }

    /**
     * Create a ProgressWatcher with the given ProgressMonitor and Monitorable.
     * <p>
     * The progress monitor will not start receiving updates until you call call
     * the {@link #start()}. As an alternative to this constructor, you can use
     * the static method {@link #watchProgress(ProgressMonitor, Monitorable)}
     * which creates a ProgressWatcher instance and starts it.
     * 
     * @param pm
     *            The Swing ProgressMonitor that should track the progress of
     *            the monitorable
     * @param monitorable
     *            The monitorable object that the ProgressWatcher monitors
     */
    public ProgressWatcher(ProgressMonitor pm, Monitorable monitorable) {
        this.pm = pm;
        this.monitorable = monitorable;
    }

    /**
     * Start the progress bar watcher to update the progress bar based 
     * on the progress of the given monitorable object. The progress bar
     * is set to update every 50 ms.
     */
    public void start() {
    	timer = new Timer(50, this);
    	timer.start();
    }

    /**
     * Creates a ProgressWatcher with the given JProgressBar and Monitorable using the 
     * {@link #ProgressWatcher(JProgressBar, Monitorable)} constructor and starts it immediately.
     * <p>
     * Also, the progress bar and label will remain visible once the monitorable is finished.
     * If you prefer to have the progress bar and label to be hidden after the monitorable is finished,
     * then use the {@link #ProgressWatcher(JProgressBar, Monitorable)} constructor, followed by
     * {@link #setHideLabelWhenFinished(boolean)} and/or {@link #setHideProgressBarWhenFinished(boolean)}
     * method calls to hide the label and/or progress bar after the Monitorable is finished, and then 
     * call {@link #start()} to start the ProgressWatcher.
     * 
     * @param bar The JProgressBar that will be used to track the progress of the Monitorable object
     * @param monitorable The Monitorable object that will be monitored to update the progress bar
     */
    public static void watchProgress(JProgressBar bar, Monitorable monitorable) {
    	ProgressWatcher watcher = new ProgressWatcher(bar, monitorable);
    	watcher.start();
    }
    
    /**
     * Creates a ProgressWatcher with the given JProgressBar, Monitorable, and JLabel using the 
     * {@link #ProgressWatcher(JProgressBar, Monitorable, JLabel)} constructor and starts it immediately.
     * 
     * Also, the progress bar and label will remain visible once the monitorable is finished.
     * If you prefer to have the progress bar and label to be hidden after the monitorable is finished,
     * then use the {@link #ProgressWatcher(JProgressBar, Monitorable, JLabel)} constructor, followed by
     * {@link #setHideLabelWhenFinished(boolean)} and/or {@link #setHideProgressBarWhenFinished(boolean)}
     * method calls to hide the label and/or progress bar after the Monitorable is finished, and then 
     * call {@link #start()} to start the ProgressWatcher.
     * 
     * @param bar The JProgressBar that will be used to track the progress of the Monitorable object
     * @param monitorable The Monitorable object that will be monitored to update the progress bar
     * @param label The label that will be displayed next to the progress bar
     */
    public static void watchProgress(JProgressBar bar, Monitorable monitorable, JLabel label) {
    	ProgressWatcher watcher = new ProgressWatcher(bar, monitorable, label);
    	watcher.start();
    }

    /**
     * Creates a ProgressWatcher with the given ProgressMonitor and Monitorable
     * using the {@link #ProgressWatcher(ProgressMonitor, Monitorable)}
     * constructor and starts it immediately.
     * 
     * @param pm
     *            The Swing ProgressMonitor that should track the progress of
     *            the given Monitorable object.
     * @param monitorable
     *            The Monitorable object that will be monitored to update the
     *            progress monitor.
     */
    public static void watchProgress(ProgressMonitor pm, Monitorable monitorable) {
    	ProgressWatcher watcher = new ProgressWatcher(pm, monitorable);
    	watcher.start();
    }
    
    /**
     * The method that is run when the timer thread notifies the ProgressWatcher. It 
     * updates the progress bar based on the status of the monitorable object. When the monitorable
     * is finished, it will hide the progress bar if {@link #hideProgressBarWhenFinished} is set to
     * true and it will hide the label if {@link #hideLabelWhenFinished} is set to true.
     */
    public void actionPerformed(ActionEvent evt) {
        // update the progress bar
        logger.debug("updating progress bar..."); //$NON-NLS-1$
        Integer jobSize = monitorable.getJobSize();
        if (bar != null) {
            if (monitorable.hasStarted()) {
                if (jobSize == null) {
                    bar.setIndeterminate(true);
                } else {
                    bar.setIndeterminate(false);
                    bar.setMaximum(jobSize.intValue());
                    bar.setValue(monitorable.getProgress());
                }
                bar.setVisible(true);
            }
            if (monitorable.isFinished() && hideProgressBarWhenFinished){
                bar.setVisible(false);
            } 
        }

        if (label != null) {
            label.setVisible(true);
            label.setText(monitorable.getMessage());
        }

        if (pm != null) { // using ProgressMonitor
            if (monitorable.hasStarted()) {					
            	monitorable.setCancelled(pm.isCanceled());
                if (jobSize != null) {
                    pm.setMaximum(jobSize.intValue());					
                }
                pm.setProgress(monitorable.getProgress());
                logger.debug("progress: " + monitorable.getProgress()); //$NON-NLS-1$
                pm.setNote(monitorable.getMessage());
            }
        }
        logger.debug("monitorable.isFinished():" + monitorable.isFinished()); //$NON-NLS-1$
        if (monitorable.isFinished()) {
            if (label != null && hideLabelWhenFinished) {
                label.setText(""); //$NON-NLS-1$
            }
            if (bar != null) {
                bar.setValue(0);
                bar.setIndeterminate(false);
            }
            if (pm != null) {
                logger.debug("pm done, max was: " + pm.getMaximum()); //$NON-NLS-1$
                pm.close();
            }

            logger.debug("trying to stop timer thread..."); //$NON-NLS-1$
            timer.stop();
            logger.debug("did the timer thread stop???"); //$NON-NLS-1$
        }
    }

    /**
     * Returns true if the progress bar will be hidden once the monitorable is finished.
     * Otherwise returns false, in that case the progress bar will remain and the value set to 0
     * when finished. The default value is false.
     */
	public boolean isHideProgressBarWhenFinished() {
		return hideProgressBarWhenFinished;
	}

    /**
     * Set if the progress bar will be hidden once the monitorable is finished.
     * The default value is false.
     */
	public void setHideProgressBarWhenFinished(boolean hideProgressBarWhenFinished) {
		this.hideProgressBarWhenFinished = hideProgressBarWhenFinished;
	}

    /**
     * Returns true if the label will be hidden once the monitorable is finished.
     * Otherwise returns false, in that case the label will remain when finished. 
     * The default value is false.
     */
	public boolean isHideLabelWhenFinished() {
		return hideLabelWhenFinished;
	}

    /**
     * Set if the label will be hidden once the monitorable is finished.
     * The default value is false.
     */
	public void setHideLabelWhenFinished(boolean hideLabelWhenFinished) {
		this.hideLabelWhenFinished = hideLabelWhenFinished;
	}
}
	
