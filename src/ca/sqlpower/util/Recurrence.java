package ca.sqlpower.util;

import java.util.*;
import java.text.*;
import org.apache.log4j.Logger;

/**
 * The Recurrence class represents the ocurrence of an event at some
 * time which can recur at some configurable interval.
 */
public class Recurrence {

	Logger logger = Logger.getLogger(Recurrence.class);

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
	 * @param baseDate The date returned will be the next occurrence
	 * after this date.
	 * @throws IllegalStateException if the recurrence frequency is
	 * unsupported.  Supported frequencies are daily, weekly, monthly,
	 * and yearly, but not quarterly.
	 */
	public Date nextOccurrence(Date baseDate) {
		logger.debug("Calc next occurrence from base="+baseDate
					 +"; start="+startDate.getTime()
					 +"; end="+(endDate==null?"never":endDate.toString()));
		// Simple base cases
		if (endDate != null && baseDate.after(endDate)) return null;
		if (baseDate.before(startDate)) return startDate;

		// ok, now we have to actually think
		Calendar next = new GregorianCalendar();  // we will alter and return this value
		next.setTime(startDate);

		switch (frequency.getFreq()) {
		default:
			long dif;
			int days;
			int weeks;
			throw new IllegalStateException("Unsupported recurrence frequency "+frequency);

		case Frequency.DAILY:
			dif = baseDate.getTime() - startDate.getTime();
			days = 1 + (int) (dif / (1000 * 60 * 60 * 24));
			logger.debug("base date is "+days+" days ("+dif+"ms) after start date");
			days = days + interval - (days % interval);
			next.add(Calendar.DATE, days);
			break;

		case Frequency.WEEKLY:
			dif = baseDate.getTime() - startDate.getTime();
			weeks = 1 + (int) (dif / (1000 * 60 * 60 * 24 * 7));
			logger.debug("base date is "+weeks+" weeks ("+dif+"ms) after start date");
			weeks = weeks + interval - (weeks % interval);
			next.add(Calendar.WEEK_OF_YEAR, weeks);
			break;
		}

		return next.getTime();
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
