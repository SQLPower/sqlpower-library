package ca.sqlpower.util;

import java.util.*;
import java.text.*;

/**
 * The Recurrence class represents the ocurrence of an event at some
 * time which can recur at some configurable interval.
 */
public class Recurrence {
	protected Calendar startDate;
	protected Calendar endDate;
	protected Frequency frequency;
	protected int interval;
	protected boolean byDate;
	protected boolean[] day;

	public Recurrence() {
		startDate = new GregorianCalendar();
		endDate = null;
		frequency = new Frequency(Frequency.DAILY);
		interval = 1;
		byDate = false;
		day = new boolean[Calendar.SATURDAY+1];
	}

	public String toString() {
		OrdinalNumberFormat ordinalFmt = new OrdinalNumberFormat();
		DateFormatSymbols dfs = new DateFormatSymbols();
		String[] weekdays = dfs.getWeekdays();
		String[] months = dfs.getMonths();
		StringBuffer sb = new StringBuffer();
		sb.append("[Recurrence: every ");
		switch (frequency.getFreq()) {
		case Frequency.DAILY:
			sb.append(interval > 1 ? interval+" days" : "day");
			break;

		case Frequency.WEEKLY:
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
				int date = startDate.get(Calendar.DATE);
				sb.append(" on the ").append(ordinalFmt.format(date));
			} else {
				int dayInMonth = startDate.get(Calendar.DAY_OF_WEEK_IN_MONTH);
				sb.append(" on the ").append(ordinalFmt.format(dayInMonth)).append(" ")
					.append(weekdays[startDate.get(Calendar.DAY_OF_WEEK)]);
			}
			break;

		case Frequency.YEARLY:
			sb.append(interval > 1 ? interval+" years" : "year");
			if (byDate) {
				int month = startDate.get(Calendar.MONTH);
				int date = startDate.get(Calendar.DATE);
				sb.append(" on ").append(months[month]).append(" ")
					.append(ordinalFmt.format(date));
			} else {
				int month = startDate.get(Calendar.MONTH);
				int dayInMonth = startDate.get(Calendar.DAY_OF_WEEK_IN_MONTH);
				sb.append(" on the ").append(ordinalFmt.format(dayInMonth)).append(" ")
					.append(weekdays[startDate.get(Calendar.DAY_OF_WEEK)])
					.append(" of ").append(months[month]);
			}
			break;
		}
		sb.append(" starting ").append(DateFormat.getDateInstance()
									   .format(startDate.getTime()));
		if (endDate != null) {
			sb.append(" until ").append(DateFormat.getDateInstance()
										.format(endDate.getTime()));
		}
		sb.append("]");
		return sb.toString();
	}


	// -------------------- Accessors and Mutators ------------------
	
	/**
	 * Gets the value of startDate
	 *
	 * @return the value of startDate
	 */
	public Calendar getStartDate()  {
		return this.startDate;
	}

	/**
	 * Sets the value of startDate
	 *
	 * @param argStartDate Value to assign to this.startDate
	 */
	public void setStartDate(Calendar argStartDate) {
		this.startDate = argStartDate;
	}

	/**
	 * Gets the value of endDate
	 *
	 * @return the value of endDate
	 */
	public Calendar getEndDate()  {
		return this.endDate;
	}

	/**
	 * Sets the value of endDate
	 *
	 * @param argEndDate Value to assign to this.endDate
	 */
	public void setEndDate(Calendar argEndDate) {
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
	 * Gets the value of interval
	 *
	 * @return the value of interval
	 */
	public int getInterval()  {
		return this.interval;
	}

	/**
	 * Sets the value of interval
	 *
	 * @param argInterval Value to assign to this.interval
	 */
	public void setInterval(int argInterval) {
		this.interval = argInterval;
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
		return frequency.getFreq() == Frequency.WEEKLY
			&& (day[dayNum] || dayNum==startDate.get(Calendar.DAY_OF_WEEK));
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
