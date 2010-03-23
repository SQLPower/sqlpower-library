package ca.sqlpower.util;

import java.util.Calendar;

import junit.framework.TestCase;

/**
 * Tests the Recurrence class.
 */
public class RecurrenceTest extends TestCase {
    
	public Recurrence r;
	private Calendar calendar;
	private Calendar calendar2;

	public void setUp() {
		r = new Recurrence();

        calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);

        calendar2 = Calendar.getInstance();
        calendar2.set(Calendar.MILLISECOND, 0);
		
		calendar.set(2004, 0, 25, 11, 30, 30); // 2004/01/25 11:30:30
		r.setStartDate(calendar.getTime());
	}
	
	/**
	 * Ensures that nextOccurrence(Date baseDate) works
	 * when the next occurrence is scheduled for later in the
	 * same day as baseDate
	 */
	public void testDailySameDay() {
		r.setFrequency(new Frequency(Frequency.DAILY));
		calendar.set(2004, 0, 26, 11, 00); // 2004/01/26 11:00
		calendar2.set(2004, 0, 26, 11, 30, 30); // 2004/01/26 11:30:30
		assertEquals(r.nextOccurrence(calendar.getTime()), calendar2.getTime());
	}

	/**
	 * Ensures that nextOccurrence(Date baseDate) works
	 * when the next occurrence is scheduled for the next day
	 */
	public void testDailyNextDay() {
		r.setFrequency(new Frequency(Frequency.DAILY));
		calendar.set(2004, 0, 26, 12, 00); // 2004/01/26 12:00
		calendar2.set(2004, 0, 27, 11, 30, 30); // 2004/01/27 11:30:30
		assertEquals(r.nextOccurrence(calendar.getTime()), calendar2.getTime());                   
	}
	
	/**
	 * Ensures that nextOccurrence(Date baseDate) works
	 * when the next occurrence is scheduled for the next day
	 * at the same time of day
	 */
	public void testDailyOnBoundary() {
		r.setFrequency(new Frequency(Frequency.DAILY));
		calendar.set(2004, 0, 27, 11, 30, 30); // 2004/01/27 11:30:30
		calendar2.set(2004, 0, 28, 11, 30, 30); // 2004/01/28 11:30:30
		assertEquals(r.nextOccurrence(calendar.getTime()), calendar2.getTime());
	}

	/**
	 * Ensures that nextOccurrence(Date baseDate) works
	 * when the next occurrence is scheduled for the same week
	 */
	public void testWeeklySameWeek() {
		r.setFrequency(new Frequency(Frequency.WEEKLY));
		calendar.set(2004, 0, 24, 12, 00); // 2004/01/24 12:00
		calendar2.set(2004, 0, 25, 11, 30, 30); // 2004/01/25 11:30:30
		assertEquals(r.nextOccurrence(calendar.getTime()), calendar2.getTime());                   
	}
	
	/**
	 * Ensures that nextOccurrence(Date baseDate) works
	 * when the next occurrence is scheduled for the same month
	 */
	public void testMonthlySameMonth() {
		r.setFrequency(new Frequency(Frequency.MONTHLY));
		calendar.set(2004, 0, 24, 12, 00); // 2004/01/24 12:00
		calendar2.set(2004, 0, 25, 11, 30, 30); // 2004/01/25 11:30:30
		assertEquals(r.nextOccurrence(calendar.getTime()), calendar2.getTime());                   
	}

	/**
	 * Ensures that nextOccurrence(Date baseDate) works
	 * when the next occurrence is scheduled for the same year
	 */
	public void testYearlySameYear() {
		r.setFrequency(new Frequency(Frequency.YEARLY));
		calendar.set(2004, 0, 24, 12, 00); // 2004/01/24 12:00
		calendar2.set(2004, 0, 25, 11, 30, 30); // 2004/01/25 11:30:30
		assertEquals(r.nextOccurrence(calendar.getTime()), calendar2.getTime());                  
	}

}
