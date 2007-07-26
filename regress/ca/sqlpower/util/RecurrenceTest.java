package ca.sqlpower.util;

import junit.framework.*;
import ca.sqlpower.util.*;
import java.util.Date;

/**
 * Tests the Recurrence class.
 */
public class RecurrenceTest extends TestCase {
	public Recurrence r;

	public RecurrenceTest() {
	}

	public RecurrenceTest(String name) {
		super(name);
	}

	public void setUp() {
		r = new Recurrence();
		r.setStartDate(new Date(104, 0, 25, 11, 30, 30));
	}

	public void testDailySameDay() {
		r.setFrequency(new Frequency(Frequency.DAILY));
		assertEquals(r.nextOccurrence(new Date(104, 0, 26, 11, 00)),
					new Date(104, 0, 26, 11, 30, 30));
	}

	public void testDailyNextDay() {
		r.setFrequency(new Frequency(Frequency.DAILY));
		assertEquals(r.nextOccurrence(new Date(104, 0, 26, 12, 00)),
					new Date(104, 0, 27, 11, 30, 30));
	}
	
	public void testDailyOnBoundary() {
		r.setFrequency(new Frequency(Frequency.DAILY));
		assertEquals(r.nextOccurrence(new Date(104, 0, 27, 11, 30, 30)),
					new Date(104, 0, 28, 11, 30, 30));
	}

}
