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
