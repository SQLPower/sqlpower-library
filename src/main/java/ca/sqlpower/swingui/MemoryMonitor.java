/*
 * Created on Sep 10, 2008
 * 
 * This code originally came from Spectro-Edit, but has been donated to
 * SQL Power by Jonathan Fuerth.
 * 
 * Copyright (c) 2009, SQL Power Group Inc.
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.Timer;

/**
 * A class for monitoring the amount of free memory and the total
 * heap size.
 */
public class MemoryMonitor {

    private Timer timer;
    private JLabel label = new JLabel();
    private long totalMemory = 0;
	private long freeMemory = 0;
	private long usedMemory = 0;
	
	
    private MouseAdapter gcMouseEvent = new MouseAdapter() {
        @Override
        @edu.umd.cs.findbugs.annotations.SuppressWarnings(value={"DM_GC"}, justification="Used as a debugging tool")
        public void mousePressed(MouseEvent e) {
            System.gc();
        }
    };

    private class MemoryUsageListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            long megabyte = 1024 * 1024;
            totalMemory = Runtime.getRuntime().totalMemory() / megabyte;
            freeMemory = Runtime.getRuntime().freeMemory() / megabyte;
            usedMemory = totalMemory - freeMemory;
            label.setText(usedMemory + "M/" + totalMemory + "M");
        }
    };
    
    public MemoryMonitor() {
        timer = new Timer(1000, new MemoryUsageListener());
        label.addMouseListener(gcMouseEvent);
    }
    
    public void start() {
        timer.start();
    }
    
    public void stop() {
        timer.stop();
    }

    /**
     * Returns a label that gives a periodically-updating reading
     * of the amount of memory in use.
     */
    public JLabel getLabel() {
        return label;
    }
    
    public long getTotalMemory() {
		return totalMemory;
	}
	
    public long getFreeMemory() {
		return freeMemory;
	}
	
    public long getUsedMemory() {
		return usedMemory;
	}
    
    public String toString() {
    	return 
    			"MemoryMonitor Status: Total:" + totalMemory 
    			+ "; Used:" + usedMemory 
    			+ "; Free:" + freeMemory;
    }
}
