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
