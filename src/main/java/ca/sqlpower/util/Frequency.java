package ca.sqlpower.util;

import java.util.LinkedList;
import java.util.List;

/**
 * The Frequency class describes a time frequency, and has methods to
 * convert between SQLPower in-database frequency codes ("YEARLY",
 * "QUARTERLY", ... "HOURLY") and SQLPower numeric frequency codes
 * (Frequency.YEARLY, Frequency.QUARTERLY, ..., Frequency.HOURLY).
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class Frequency implements Comparable, java.io.Serializable {
	
	/**
	 * A frequency of once per year.
	 */
	public static final int YEARLY = 1;

	/**
	 * A frequency of once per quarter-year (every three months).
	 */
	public static final int QUARTERLY = 2;

	/**
	 * A frequency of once per month.
	 */
	public static final int MONTHLY = 3;

	/**
	 * A frequency of once per week (every seven days).
	 */
	public static final int WEEKLY = 4;

	/**
	 * A frequency of once per day (every 24 hours).
	 */
	public static final int DAILY = 5;

	/**
	 * A frequency of once per hour.
	 */
	public static final int HOURLY = 6;

	/**
	 * The minimum frequency currently supported (YEARLY).
	 */
	public static final int MIN_FREQ = YEARLY;

	/**
	 * The maximum frequency currently supported (DAILY).
	 */
	public static final int MAX_FREQ = DAILY;

	/**
	 * The frequency code for this instance's current frequency.
	 */
	protected int freq;


	// ---------------- STATIC METHODS ----------------

	/**
	 * Converts a frequency unit string (such as "MONTHLY" or "MONTH")
	 * into an integer code (such as Frequency.MONTHLY).
	 *
	 * @param fc A frequency unit string, or <code>null</code>.
	 * @return The frequency code corresponding to the given string,
	 * or <code>Frequency.MONTHLY</code> if the code is unknown or
	 * <code>null</code>.
	 */
	public static int freqCodeToFreq(String fc) throws UnknownFreqCodeException {
		if (fc == null) {
			fc="";
		}
		fc = fc.toUpperCase();
		if (fc.equals("YEARLY") || fc.equals("YEAR")) return Frequency.YEARLY;
		if (fc.equals("QUARTERLY") || fc.equals("QUARTER")) return Frequency.QUARTERLY;
		if (fc.equals("MONTHLY") || fc.equals("MONTH")) return Frequency.MONTHLY;
		if (fc.equals("WEEKLY") || fc.equals("WEEK")) return Frequency.WEEKLY;
		if (fc.equals("DAILY") || fc.equals("DAY")) return Frequency.DAILY;
		if (fc.equals("HOURLY") || fc.equals("HOUR")) return Frequency.HOURLY;
		throw new UnknownFreqCodeException("unknown freq code: " + fc);
	}

	/**
	 * Converts a SQLPower numeric frequency code to its corresponding
	 * SQLPower database frequency code.
	 */
	public static String freqToFreqCode(int freq)
	  throws UnknownFreqCodeException {
		if (freq == Frequency.YEARLY) return "YEARLY";
		if (freq == Frequency.QUARTERLY) return "QUARTERLY";
		if (freq == Frequency.MONTHLY) return "MONTHLY";
		if (freq == Frequency.WEEKLY) return "WEEKLY";
		if (freq == Frequency.DAILY) return "DAILY";
		if (freq == Frequency.HOURLY) return "HOURLY";
		throw new UnknownFreqCodeException("unknown freq: " + freq);
	}

	/**
	 * Creates the list of SQLPower frequencies which are equal to or
	 * less frequent than the given frequencies.
	 */
	public static List getListOfFreqsLessThan(Frequency maxFreq) {
		int freqCode = maxFreq.getFreq();
		List freqList = new LinkedList();
		do {
			freqList.add(new Frequency(freqCode));
			freqCode--;
		} while (freqCode >= MIN_FREQ);
		return freqList;
	}


	// ---------------- CONSTRUCTORS ----------------

	public Frequency(int freqCode) {
		freq = freqCode;
	}


	// ---------------- INSTANCE METHODS ----------------

	/**
	 * Gets the numeric frequency code for this frequency instance.
	 *
	 * @return A value between MIN_FREQ and MAX_FREQ.
	 */
	public int getFreq() {
		return this.freq;
	}

	/**
	 * Sets the numeric frequency code for this frequency instance.
	 *
	 * @param argFreq A value between MIN_FREQ and MAX_FREQ.
	 */
	public void setFreq(int argFreq){
		this.freq = argFreq;
	}

	@Override
	public String toString() {
		if (freq == Frequency.YEARLY) return "YEARLY";
		if (freq == Frequency.QUARTERLY) return "QUARTERLY";
		if (freq == Frequency.MONTHLY) return "MONTHLY";
		if (freq == Frequency.WEEKLY) return "WEEKLY";
		if (freq == Frequency.DAILY) return "DAILY";
		if (freq == Frequency.HOURLY) return "HOURLY";
		throw new IllegalStateException("[Frequency: Invalid frequency code " + freq + "]");
	}

	/**
	 * Only compares to other Frequencies.
	 *
	 * @throws ClassCastException if <code>other</code> is not a
	 * <code>Frequency</code>.
	 */
	@Override
	public boolean equals(Object other) throws ClassCastException {
		if (other != null && ((Frequency) other).freq == this.freq) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * 17 + freq;
		return result;
	}

	/**
	 * Only compares to other Frequencies. <code>YEARLY</code> is the
	 * minimum, HOURLY is the maximum.
	 *
	 * @throws ClassCastException if <code>other</code> is not a
	 * <code>Frequency</code>.
	 */
	public int compareTo(Object other) throws ClassCastException {
		return this.freq - ((Frequency) other).freq;
	}
}
