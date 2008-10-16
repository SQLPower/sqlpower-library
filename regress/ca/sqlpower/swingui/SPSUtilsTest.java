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

import java.awt.Polygon;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import junit.framework.TestCase;

public class SPSUtilsTest extends TestCase {
   
    public void testCreateSimpleUpArrowhead() {
        Polygon p = SPSUtils.createArrowhead(20, 20, 20, 60, 10, 10);
       
        assertEquals(3, p.npoints);
        System.out.println("points are x1 " + p.xpoints[0] + " x2 " +
                p.xpoints[1] + " x3 " + p.xpoints[2] + " y1 " + p.ypoints[0] +
                " y2 " + p.ypoints[1] + " y3 " + p.ypoints[2]);
        assertTrue(p.contains (20, 21));
        assertTrue(p.contains(16, 29));
        assertTrue(p.contains(24, 29));
    }
   
    public void testCreateSimpleSidewaysArrowhead() {
        Polygon p = SPSUtils.createArrowhead (20, 20, 60, 20, 10, 10);
       
        assertEquals(3, p.npoints);
       
        System.out.println("points are x1 " + p.xpoints[0] + " x2 " +
                p.xpoints[1] + " x3 " + p.xpoints[2] + " y1 " + p.ypoints[0] +
                " y2 " + p.ypoints[1] + " y3 " + p.ypoints[2]);
        assertTrue(p.contains(21, 20));
        assertTrue(p.contains(29, 16));
        assertTrue(p.contains(29, 24));
    }
   
    public void testCreateSimpleDiagonalArrowhead() {
        Polygon p = SPSUtils.createArrowhead(20, 20, 60, 60, 10, 10);
       
        assertEquals(3, p.npoints);
        System.out.println("points are x1 " + p.xpoints[0] + " x2 " +
                p.xpoints[1] + " x3 " + p.xpoints[2] + " y1 " + p.ypoints[0] +
                " y2 " + p.ypoints[1] + " y3 " + p.ypoints[2]);
        assertTrue(p.contains(21, 21));
        assertTrue(p.contains(29, 24));
        assertTrue(p.contains(23, 30));
    }
    
	public void testClassNameStuff() {
        assertEquals("String", SPSUtils.niceClassName(""));
        assertEquals("Object", SPSUtils.niceClassName(new Object()));
    }
    
    /**
     * Tests to make sure that makeOwnedDialog checks the given component
     * to see if it is a Window. This was a previous bug where the method
     * only checked for the ancestors of the given component and not the
     * component itself.
     */
    public void testMakeOwnedDialog() {
    	JDialog dialog = null;
    	JFrame frame = new JFrame();
    	JPanel panel = new JPanel();
    	frame.add(panel);
    	
    	dialog = SPSUtils.makeOwnedDialog(panel, "test");
    	assertEquals("Dialog should be owned!", frame, dialog.getParent());
    	dialog = SPSUtils.makeOwnedDialog(frame, "title");
    	assertEquals("Dialog should be owned!", frame, dialog.getParent());
    }
    

    public void testBreakLongMenu() throws Exception {
        final JFrame jf = new JFrame();
        jf.setSize(400, 400);
        final JMenuBar jb = new JMenuBar();
        jf.setJMenuBar(jb);
        final JMenu fileMenu = new JMenu("File");
        int itemsInMenu = 30;
        for (int i = 0; i <= itemsInMenu; i++) {
            fileMenu.add(new JMenuItem(Integer.toString(i)));
        }

        jb.add(fileMenu);
        
        assertTrue("The test didn't put enough items in the menu to make the test effective (prefsize="+fileMenu.getPopupMenu().getPreferredSize()+")",
                fileMenu.getPopupMenu().getPreferredSize().height > jf.getHeight());

        SPSUtils.breakLongMenu(jf, fileMenu);

        assertTrue("Breaking up the menu didn't reduce the item count!",
                fileMenu.getItemCount() < itemsInMenu);
        
        assertSame("The last item wasn't a submenu!",
                JMenu.class, fileMenu.getItem(fileMenu.getItemCount() - 1).getClass());
        
    }
}
