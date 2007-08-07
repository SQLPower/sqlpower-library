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
     * Create a ProgressWatcher with the given progress bar and Monitorable, and set the 
     * progress bar label to the given JLabel. The label and progress bar will remain visible
     * after the monitorable is finished by default.  
     * <p>
     * The progress bar will not start up at this point. You should either use the static method
     * {@link #watchProgress(JProgressBar, Monitorable, JLabel))} or call the {@link #start()}
     * method after calling the constructor to start the progress bar monitor.
     * 
     * @param bar The JProgressBar that is tracking the progress of the monitorable
     * @param monitorable The monitorable object that the ProgressWatcher monitors
     * @param label The label you want to display on the progress bar 
     */
    public ProgressWatcher(JProgressBar bar, Monitorable monitorable, JLabel label) {
        this.bar = bar;
        this.monitorable = monitorable;
        this.label = label;
    }

    /**
     * Create a ProgressWatcher with the given ProgressMonitor and Monitorable.
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
     * Creates a ProgressWatcher with the given ProgressMonitor and Monitorable using the 
     * {@link #ProgressWatcher(ProgressMonitor, Monitorable)} constructor and starts it immediately.
     *
     * Also, the progress bar and label will remain visible once the monitorable is finished.
     * If you prefer to have the progress bar and label to be hidden after the monitorable is finished,
     * then use the {@link #ProgressWatcher(ProgressMonitor, Monitorable)} constructor, followed by
     * {@link #setHideLabelWhenFinished(boolean)} and/or {@link #setHideProgressBarWhenFinished(boolean)}
     * method calls to hide the label and/or progress bar after the Monitorable is finished, and then 
     * call {@link #start()} to start the ProgressWatcher.
     * 
     * @param bar The ProgressMonitor that will used to monitor the progress of the Monitorable object
     * @param monitorable The Monitorable object that will be monitored to update the progress bar
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
        logger.debug("updating progress bar...");
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
        }

        if (pm != null) { // using ProgressMonitor
            if (monitorable.hasStarted()) {					
                if (jobSize != null) {
                    pm.setMaximum(jobSize.intValue());					
                }
                pm.setProgress(monitorable.getProgress());
                logger.debug("progress: " + monitorable.getProgress());
                pm.setNote(monitorable.getMessage());
            }
        }
        logger.debug("monitorable.isFinished():" + monitorable.isFinished());
        if (monitorable.isFinished()) {
            if (label != null && hideLabelWhenFinished) {
                label.setText("");
            }
            if (bar != null) {
                bar.setValue(0);
                bar.setIndeterminate(false);
            }
            if (pm != null) {
                logger.debug("pm done, max was: " + pm.getMaximum());
                pm.close();
            }

            logger.debug("trying to stop timer thread...");
            timer.stop();
            logger.debug("did the timer thread stop???");
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
	
