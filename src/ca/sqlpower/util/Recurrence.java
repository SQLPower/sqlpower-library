package ca.sqlpower.util;

import java.util.*;
import java.text.*;
import org.apache.log4j.Logger;

/**
 * The Recurrence class represents the ocurrence of an event at some
 * time which can recur at some configurable interval.
 */
public class Recurrence {

	private static Logger logger = Logger.getLogger(Recurrence.class);

	/**
	 * This is the date and time of the first occurrence.  The
	 * time-of-day component also determines the time of day for
	 * future occurrences.
	 */
	protected Date startDate;

	/**
	 * This is an optional end date (and time) past which no further
	 * occurrences are scheduled.
	 */
	protected Date endDate;

	/**
	 * This is the recurrence frequency.  Supported values are
	 * Frequency.DAILY, Frequency.WEEKLY, Frequency.MONTHLY,
	 * Frequency.YEARLY.  Note that QUARTERLY is not supported.
	 */
	protected Frequency frequency;

	/**
	 * This is the interval (based on the current frequency) of
	 * recurrence.  For example, if frequency is MONTHLY and interval
	 * is 2, the recurrence will be scheduled every two months.
	 */
	protected int interval;

	/**
	 * <code>byDate</code> modifies the way monthly and yearly
	 * recurrences work.  If startDate is December 18, 2003 (the third
	 * Thursday of December 2003) and frequency is <code>YEARLY</code>, a
	 * recurrence <code>byDate</code> would be December 18, 2004 (not a Thursday).
	 * On the other hand, if <code>byDate</code> was
	 * <code>false</code>, the next occurrence would be December 16,
	 * 2004 (the third Thursday).
	 */
	protected boolean byDate;

	/**
	 * For WEEKLY recurrences, this array holds the days of the week
	 * that the recurrence will happen on.  These values are not
	 * returned directly by {@link #isOnDay(int)}, because the day of
	 * the week for startDate is always a recurrence day.
	 */
	protected boolean[] day;

	/**
	 * Sets up a new recurrence which is initially scheduled every day
	 * at the current time of day forever.
	 */
	public Recurrence() {
		startDate = new Date();
		endDate = null;
		frequency = new Frequency(Frequency.DAILY);
		interval = 1;
		byDate = false;
		day = new boolean[Calendar.SATURDAY+1];
	}

	/**
	 * Prints out an English, human-readable representation of this recurrence.
	 */
	public String toString() {
		OrdinalNumberFormat ordinalFmt = new OrdinalNumberFormat();
		DateFormatSymbols dfs = new DateFormatSymbols();
		String[] weekdays = dfs.getWeekdays();
		String[] months = dfs.getMonths();
		StringBuffer sb = new StringBuffer();
		Calendar startCal = new GregorianCalendar();
		startCal.setTime(startDate);
		sb.append("[Recurrence: every ");
		switch (frequency.getFreq()) {
		case Frequency.DAILY:
			sb.append(interval > 1 ? interval+" days" : "day");
			break;

		case Frequency.WEEKLY:
			if (interval > 1) {
				sb.append(interval+" weeks on ");
			}
			for (int d = Calendar.SUNDAY, n = 0; d <= Calendar.SATURDAY; d++) {
				if (isOnDay(d)) {
					if (n>0) sb.append(", ");
					sb.append(weekdays[d]);
					n++;
				}
			}
			break;

		case Frequency.MONTHLY:
			sb.append(interval > 1 ? interval+" months" : "month");
			if (byDate) {
				int date = startCal.get(Calendar.DATE);
				sb.append(" on the ").append(ordinalFmt.format(date));
			} else {
				int dayInMonth = startCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);
				sb.append(" on the ").append(ordinalFmt.format(dayInMonth)).append(" ")
					.append(weekdays[startCal.get(Calendar.DAY_OF_WEEK)]);
			}
			break;

		case Frequency.YEARLY:
			sb.append(interval > 1 ? interval+" years" : "year");
			if (byDate) {
				int month = startCal.get(Calendar.MONTH);
				int date = startCal.get(Calendar.DATE);
				sb.append(" on ").append(months[month]).append(" ")
					.append(ordinalFmt.format(date));
			} else {
				int month = startCal.get(Calendar.MONTH);
				int dayInMonth = startCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);
				sb.append(" on the ").append(ordinalFmt.format(dayInMonth)).append(" ")
					.append(weekdays[startCal.get(Calendar.DAY_OF_WEEK)])
					.append(" of ").append(months[month]);
			}
			break;

		default:
			throw new IllegalStateException("Unsupported recurrence frequency "+frequency);
		}
		sb.append(" starting ").append(DateFormat.getDateInstance()
									   .format(startDate));
		if (endDate != null) {
			sb.append(" until ").append(DateFormat.getDateInstance()
										.format(endDate));
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Computes and returns the date and time of the next occurrence
	 * of this Recurrence instance, relative to the current system date.
	 */
	public Date nextOccurrence() {
		return nextOccurrence(new Date());
	}

	/**
	 * Computes and returns the date and time of the next occurrence
	 * of this Recurrence instance, relative to an arbitrary base date.
	 *
	 * <p>Calculating the next occurrence is a constant time operation
	 * for all supported frequencies, intervals, and date settings.
	 *
	 * @param baseDate The date returned will be the next occurrence
	 * after this date.
	 * @throws IllegalStateException if the recurrence frequency is
	 * unsupported.  Supported frequencies are daily, weekly, monthly,
	 * and yearly, but not quarterly.
	 */
	public Date nextOccurrence(Date baseDate) {
		logger.debug("Calc next occurrence from base="+baseDate
					 +"; start="+startDate
					 +"; end="+(endDate==null?"never":endDate.toString()));
		// Simple base cases
		if (endDate != null && baseDate.after(endDate)) return null;
		if (baseDate.before(startDate)) return startDate;

		// ok, now we have to actually think (well, *I* have to think. the computer just runs instructions either way)
		Calendar next = new GregorianCalendar();  // we will alter and return this value
		next.setTime(startDate);

		switch (frequency.getFreq()) {
		default:
			long dif;
			int days;
			int weeks;
			int months;
			int years;
			GregorianCalendar base;
			GregorianCalendar start;
			throw new IllegalStateException("Unsupported recurrence frequency "+frequency);

		case Frequency.DAILY:
			// figures out next occurrence by calculating days from start until base
			dif = baseDate.getTime() - startDate.getTime();
			days = 1 + (int) (dif / (1000 * 60 * 60 * 24));
			logger.debug("base date is "+days+" days ("+dif+"ms) after start date");
			if (interval > 1) {
				days = days + interval - (days % interval);
			}
			next.add(Calendar.DATE, days);
			break;

		case Frequency.WEEKLY:
			/* this one is the most complicated because it's different.
			 * General idea:
			 * 1. Let w be the calendar week of base
			 * 2. Let ww be the closest scheduled interval week on or after w
			 * 3. let d be { w==ww: day of week of base; w!=ww: sunday }
			 * 4. let dd be the next recurrence day after d. if there are no more
			 *    recurrence days after d, let dd be the first recurrence day.
			 * 5. let www be { w==ww and dd < d: ww + interval; else: ww }
			 * 6. set next week = www; day of week = dd.
			 *
			 * Note: when I say "recurrence week" or "recurrence day" I mean a week
			 * or day of week for which the user has scheduled an occurrence of this
			 * object.  For example, if interval == 2, then every other week is a 
			 * recurrence week.
			 */
			start = new GregorianCalendar();
			start.setTime(startDate);
			start.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
			start.set(Calendar.HOUR, 0);
			start.set(Calendar.MINUTE, 0);
			start.set(Calendar.SECOND, 0);
			start.set(Calendar.MILLISECOND, 0);

			base = new GregorianCalendar();
			base.setTime(baseDate);

			// calculate weeks from start to base (required for determining ww)
			dif = baseDate.getTime() - start.getTime().getTime();
			weeks = (int) (dif / (1000 * 60 * 60 * 24 * 7));
			logger.debug("base date is "+weeks+" weeks ("+dif+"ms) after start date");

			int w = start.get(Calendar.WEEK_OF_YEAR) + weeks;  // base week in start year
			int ww; // next scheduled recurrence week (will be w if w is a recurrence week)
			if ((interval > 1) && (weeks % interval != 0)) {
				ww = start.get(Calendar.WEEK_OF_YEAR) + (weeks + interval - (weeks % interval));
			} else {
				ww = w;
			}

			int d; // Calendar.SUNDAY == 1 .. Calendar.SATURDAY == 7
			if (w == ww) {
				d = base.get(Calendar.DAY_OF_WEEK);
			} else {
				d = Calendar.SUNDAY;
			}

			int dd = d;
			do {
				dd += 1;
				if (dd > Calendar.SATURDAY) dd = Calendar.SUNDAY;
			} while (!isOnDay(dd));

			int www;
			if (w == ww && dd <= d) {
				// day of week wrapped around, so we're into the next week now.
				www = ww + interval;
			} else {
				www = ww;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Base week = "+w);				
				logger.debug("Next Recurrence week = "+ww);
				logger.debug("Searching day-of-week starting with "
							 +new DateFormatSymbols().getWeekdays()[d]);
				logger.debug("Selected next day-of-week "
							 +new DateFormatSymbols().getWeekdays()[dd]);
				logger.debug("Final choice of next week-of-year "+www);
			}
			next.set(Calendar.DAY_OF_WEEK, dd);
			next.set(Calendar.WEEK_OF_YEAR, www); // bridges to new year with www > 52
			break;

		case Frequency.MONTHLY:
			/* not quite as easy as DAILY because months have variable
			 * length and there is a user-settable byDate mode that
			 * only affects MONTHLY and YEARLY.
			 */
			base = new GregorianCalendar();
			base.setTime(baseDate);
			start = new GregorianCalendar();
			start.setTime(startDate);
			
			months = monthsFromStart(base);  // properly handles both byDate modes
			logger.debug("months from start to base = "+months);
			if (months % interval != 0) {
				months = months + interval - (months % interval);
			}
			logger.debug("next occurrence will be "+months+" months after start");

			next.set(Calendar.DATE, 1); // fake date for now; it is set after bumping the month
			next.add(Calendar.MONTH, months);

			if (byDate) {
				next.set(Calendar.DATE, start.get(Calendar.DATE));
			} else {
				next.set(Calendar.DAY_OF_WEEK, start.get(Calendar.DAY_OF_WEEK));
				next.set(Calendar.DAY_OF_WEEK_IN_MONTH, start.get(Calendar.DAY_OF_WEEK_IN_MONTH));
			}

			break;

		case Frequency.YEARLY:
			// works similarly to the MONTHLY case
			base = new GregorianCalendar();
			base.setTime(baseDate);
			start = new GregorianCalendar();
			start.setTime(startDate);
			
			months = monthsFromStart(base);  // properly handles both byDate modes
			logger.debug("months from start to base = "+months);
			years = months / 12;
			if (months % 12 != 0) {
				years += 1;
			}
			if (years % interval != 0) {
				years = years + interval - (years % interval);
			}
			logger.debug("next occurrence will be "+years+" years after start");

			next.set(Calendar.DATE, 1); // fake date for now; it is set after bumping the year
			next.add(Calendar.YEAR, years);

			if (byDate) {
				next.set(Calendar.DATE, start.get(Calendar.DATE));
			} else {
				next.set(Calendar.DAY_OF_WEEK, start.get(Calendar.DAY_OF_WEEK));
				next.set(Calendar.DAY_OF_WEEK_IN_MONTH, start.get(Calendar.DAY_OF_WEEK_IN_MONTH));
			}
			break;
		}

		return next.getTime();
	}

	/**
	 * Calculates and returns the number of milliseconds from midnight
	 * to the time-of-day stored in cal.
	 *
	 * @param cal A Calendar instance.  This method only uses its
	 * time-of-day fields.
	 * @return An integer between 0 and 86499999 inclusive.
	 */
	protected int msSinceMidnight(Calendar cal) {
		int ms = cal.get(Calendar.MILLISECOND);
		ms += 1000 * cal.get(Calendar.SECOND);
		ms += 1000 * 60 * cal.get(Calendar.MINUTE);
		ms += 1000 * 60 * 60 * cal.get(Calendar.HOUR);
		return ms;
	} 

	/**
	 * Calculates how many months are between this Recurrence's start
	 * date and the given calendar.  Partial months are counted as
	 * full months.  For example, there is 1 month between 2004-01-02
	 * and 2004-02-01 and 1 month between 2004-01-01 and 2004-01-02.
	 * There are 3 months between 2003-12-01 and 2004-02-14.
	 *
	 * <p>This method behaves differently depending on the setting of
	 * <code>byDate</code>.  If <code>byDate</code> is set, the month
	 * boundary is the same time-of-day on the same date of following
	 * months.  For instance, the month following 2004-01-06 10:00
	 * starts at 2004-02-06 10:00.  The next month after that starts
	 * at 2004-03-06 10:00. If <code>byDate</code> is not set, then
	 * the month boundary is at the same time-of-day on the same
	 * day-of-week-in-month of following months.  For instance, the
	 * month following 2004-01-06 10:00 (the first Tuesday of January)
	 * starts at 2004-02-03 10:00 (the first Tuesday of February).
	 * The next following month starts at 2004-03-02 (the first
	 * Tuesday of March).
	 *
	 * @param second a date after this recurrence's start date.
	 * @return the number of months from the start date to the given
	 * date.  If cal represents a moment in time before this.startDate,
	 * the return value is undefined.
	 */
	public int monthsFromStart(Calendar cal) {
		GregorianCalendar start = new GregorianCalendar();
		start.setTime(startDate);
		int months = 12 * (cal.get(Calendar.YEAR) - start.get(Calendar.YEAR));
		months += cal.get(Calendar.MONTH) - start.get(Calendar.MONTH);
		
		if (byDate) {
			if (start.get(Calendar.DATE) < cal.get(Calendar.DATE)
				|| (start.get(Calendar.DATE) == cal.get(Calendar.DATE)
					&& msSinceMidnight(start) <= msSinceMidnight(cal))) {
				months += 1;
			}
		} else {
			logger.debug("start DAY_OF_WEEK_IN_MONTH "+start.get(Calendar.DAY_OF_WEEK_IN_MONTH)
						 +"; DAY_OF_WEEK "+start.get(Calendar.DAY_OF_WEEK));
			logger.debug("cal   DAY_OF_WEEK_IN_MONTH "+cal.get(Calendar.DAY_OF_WEEK_IN_MONTH)
						 +"; DAY_OF_WEEK "+cal.get(Calendar.DAY_OF_WEEK));
			if (start.get(Calendar.DAY_OF_WEEK_IN_MONTH) < cal.get(Calendar.DAY_OF_WEEK_IN_MONTH)
				|| (start.get(Calendar.DAY_OF_WEEK_IN_MONTH) == cal.get(Calendar.DAY_OF_WEEK_IN_MONTH)
					&& start.get(Calendar.DAY_OF_WEEK) < cal.get(Calendar.DAY_OF_WEEK)
					|| (start.get(Calendar.DAY_OF_WEEK) == cal.get(Calendar.DAY_OF_WEEK)
						&& msSinceMidnight(start) <= msSinceMidnight(cal)))) {
				months += 1;
			}
		}
		return months;
	}

	// -------------------- Accessors and Mutators ------------------
	
	/**
	 * Gets the value of startDate
	 *
	 * @return the value of startDate
	 */
	public Date getStartDate()  {
		return this.startDate;
	}

	/**
	 * Sets the value of startDate
	 *
	 * @param argStartDate Value to assign to this.startDate
	 */
	public void setStartDate(Date argStartDate) {
		this.startDate = argStartDate;
	}

	/**
	 * Gets the value of endDate
	 *
	 * @return the value of endDate
	 */
	public Date getEndDate()  {
		return this.endDate;
	}

	/**
	 * Sets the value of endDate
	 *
	 * @param argEndDate Value to assign to this.endDate
	 */
	public void setEndDate(Date argEndDate) {
		this.endDate = argEndDate;
	}

	/**
	 * Gets the value of frequency
	 *
	 * @return the value of frequency
	 */
	public Frequency getFrequency()  {
		return this.frequency;
	}

	/**
	 * Sets the value of frequency
	 *
	 * @param argFrequency Value to assign to this.frequency
	 */
	public void setFrequency(Frequency argFrequency) {
		this.frequency = argFrequency;
	}

	/**
	 * Gets the recurrence interval.  See {@link #interval}.
	 *
	 * @return the value of interval
	 */
	public int getInterval()  {
		return this.interval;
	}

	/**
	 * Sets the recurrence interval.  See {@link #interval}.
	 *
	 * @param argInterval A positive integer.
	 * @throws IllegalArgumentException if argInterval < 1.
	 */
	public void setInterval(int argInterval) {
		if (argInterval < 1) {
			throw new IllegalArgumentException("Interval must be a positive integer");
		} else {
			this.interval = argInterval;
		}
	}

	/**
	 * Gets the value of byDate
	 *
	 * @return the value of byDate
	 */
	public boolean isByDate()  {
		return this.byDate;
	}

	/**
	 * Sets the value of byDate
	 *
	 * @param argByDate Value to assign to this.byDate
	 */
	public void setByDate(boolean argByDate) {
		this.byDate = argByDate;
	}

	/**
	 * Figures out whether or not this recurrence happens on a given
	 * day of the week.  A weekly recurrence always occurs on the
	 * day-of-week of its start date, plus any other days as set using
	 * {@link #setOnDay(int,boolean)}.  This method is only useful
	 * when the recurrence frequency is WEEKLY; returns false when
	 * this recurrence is set to any other frequency.
	 *
	 * @param dayNum A constant from java.util.Calendar representing
	 * the day of the week such as Calendar.TUESDAY.
	 * @throws IndexOutOfBoundsException if dayNum is not a valid
	 * day of the week.
	 */
	public boolean isOnDay(int dayNum) {
		if (frequency.getFreq() == Frequency.WEEKLY) {
			GregorianCalendar startCal = new GregorianCalendar();
			startCal.setTime(startDate);
			return day[dayNum] || dayNum == startCal.get(Calendar.DAY_OF_WEEK);
		} else {
			return false;
		}
	}
	
	/**
	 * Sets or clears the given day-of-week for this recurrence.
	 * Trying to turn off the day of the week that startDate falls on
	 * has no effect; the other six days of the week can be switched
	 * on and off at your whim.
	 */
	public void setOnDay(int dayNum, boolean v) {
		day[dayNum] = v;
	}
}
