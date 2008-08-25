/*
 * Originally by Jonathan Fuerth. Copyright for this copy assigned
 * to SQL Power on July 27, 2007.
 * 
 * Copyright (c) 2007, SQL Power
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
 *     * Neither the name of Jonathan Fuerth nor the names of other
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

/**
 * An icon that takes up space but doesn't paint anything. This is helpful, for
 * example, in a tree cell renderer where some of the items don't have icons and
 * some do. Without this placeholder, the tree items will not appear to nest
 * properly.
 */
public class BlankIcon implements Icon {
    
    private static final Map<Dimension, BlankIcon> instances = new HashMap<Dimension, BlankIcon>();
    
    /**
     * This icon's width and height in pixels.
     */
    private final Dimension size;
    
    /**
     * Returns a BlankIcon with the given dimensions. If there was already
     * a BlankIcon created with the requested size, that instance will be
     * returned. Otherwise, a new one will be created for you.
     * 
     * @param width The width of the icon
     * @param height The height of the icon
     */
    public static BlankIcon getInstance(int width, int height) {
        Dimension size = new Dimension(width, height);
        BlankIcon instance = instances.get(size);
        if (instance == null) {
            instance = new BlankIcon(size);
            instances.put(size, instance);
        }
        return instance;
    }
    
    /**
     * Use {@link #getInstance(int, int)} to get an instance of this class.
     */
    private BlankIcon(Dimension size) {
        this.size = size;
    }
    
    public int getIconHeight() {
        return size.height;
    }

    public int getIconWidth() {
        return size.width;
    }

    /**
     * Does nothing. This is a blank icon, remember?
     */
    public void paintIcon(Component c, Graphics g, int x, int y) {
        // no op!
    }

}
