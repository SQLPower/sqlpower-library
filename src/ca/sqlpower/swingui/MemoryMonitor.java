/*
 * Created on Sep 10, 2008
 * 
 * This code originally came from Spectro-Edit, but has been donated to
 * SQL Power by Jonathan Fuerth.
 * 
 * Copyright (c) 2008, SQL Power Group Inc.
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
    
    private ActionListener timerAction = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            long megabyte = 1024 * 1024;
            long totalMemory = Runtime.getRuntime().totalMemory() / megabyte;
            long freeMemory = Runtime.getRuntime().freeMemory() / megabyte;
            long usedMemory = totalMemory - freeMemory;
            label.setText(usedMemory + "M/" + totalMemory + "M");
        }
    };
    
    public MemoryMonitor() {
        timer = new Timer(1000, timerAction);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                System.gc();
            }
        });
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
}
