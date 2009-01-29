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
package ca.sqlpower.validation.swingui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;

import ca.sqlpower.validation.RegExValidator;

import junit.framework.TestCase;

public class TestFormValidationHandler extends TestCase {

    private FormValidationHandler handler;

    /**
     * Some test methods count events. They do so using this variable.
     */
    private int eventCount = 0;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        handler = new FormValidationHandler(new StatusComponent());
    }

    /**
     * This test actually verifies our understanding of how JTextField document
     * events work, not so much how the validation handler responds to them.
     */
    public void testEventSupport() throws BadLocationException {
        RegExValidator val = new RegExValidator("\\d+");
        final JTextField textField = new JTextField();

        final PropertyChangeListener listener = new PropertyChangeListener(){
            public void propertyChange(PropertyChangeEvent evt) {
                System.out.println("Property change!");
                new Exception().printStackTrace();
                eventCount++;
            }};
        handler.addPropertyChangeListener(listener);
        handler.addValidateObject(textField,val);

        // Note: TextField can fire 1 or 2 events for setText.
        // First it clears the old string, then it inserts the new one.
        
        // Originally, the validation handler would not revalidate the form after every
        // addValidateObject() call.  This was changed recently to work around a bug
        // in MatchMaker.  However, we have concerns about how this might impact performance
        // when adding a large number of validate objects.  The longer we wait before
        // changing the policy back, the more likely we will have additional code that
        // relies on the new policy.
        //
        // When and if we change back, the following test should expect eventCount == 0
        assertEquals("Expected one event from initial validation", 1, eventCount);

        // no need for clear
        eventCount = 0;
        textField.setText("10");
        assertEquals(1, eventCount);

        // clear and insert
        eventCount = 0;
        textField.setText("20");
        assertEquals(2, eventCount);

        // just clear
        eventCount = 0;
        textField.setText("");
        assertEquals(1, eventCount);

        // just insert
        eventCount = 0;
        textField.setText("a");
        assertEquals(1, eventCount);

        eventCount = 0;
        handler.removePropertyChangeListener(listener);
        textField.getDocument().insertString(textField.getDocument().getLength(),"aa",null);
        assertEquals("event counter should be unaffected after listener is detached", 0, eventCount);
    }
}
