package ca.sqlpower.util;

import java.util.*;


public class Frequency implements Comparable, java.io.Serializable {
	public static final int YEARLY=1;
	public static final int QUARTERLY=2;
	public static final int MONTHLY=3;
	public static final int WEEKLY=4;
	public static final int DAILY=5;
	public static final int HOURLY=6;

	public static final int MIN_FREQ=YEARLY;
	public static final int MAX_FREQ=DAILY;

	protected int freq;

	public Frequency(int freqCode) {
		freq=freqCode;
	}

	/**
	 * Gets the value of freq
	 *
	 * @return the value of freq
	 */
	public int getFreq() {
		return this.freq;
	}

	/**
	 * Sets the value of freq
	 *
	 * @param argFreq Value to assign to this.freq
	 */
	public void setFreq(int argFreq){
		this.freq = argFreq;
	}

	/**
	 * Only compares to other Frequencies.  <code>YEARLY</code> is the
	 * minimum, HOURLY is the maximum.
	 *
	 * @throws ClassCastException if <code>other</code> is not a
	 * <code>Frequency</code>.
	 */
	public int compareTo(Object other) throws ClassCastException {
		return this.freq - ((Frequency)other).freq;
	}

	public static List getListOfFreqsLessThan(Frequency maxFreq) {
		int freqCode=maxFreq.getFreq();
		List freqList=new LinkedList();
		do {
			freqList.add(new Frequency(freqCode));
			freqCode--;
		} while(freqCode >= MIN_FREQ);
		return freqList;
	}
	
	/**
	 * Only compares to other Frequencies.
	 *
	 * @throws ClassCastException if <code>other</code> is not a
	 * <code>Frequency</code>.
	 */
	public boolean equals(Object other) throws ClassCastException {
		if(((Frequency)other).freq == this.freq) {
			return true;
		} else {
			return false;
		}
	}

	public String toString() {
		if(freq==Frequency.YEARLY) return "YEARLY";
		if(freq==Frequency.QUARTERLY) return "QUARTERLY";
		if(freq==Frequency.MONTHLY) return "MONTHLY";
		if(freq==Frequency.WEEKLY) return "WEEKLY";
		if(freq==Frequency.DAILY) return "DAILY";
		if(freq==Frequency.HOURLY) return "HOURLY";
		throw new IllegalStateException("Somehow an invalid frequency code was set.");
	}

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
		if(fc==null) {
			fc="";
		}
		fc = fc.toUpperCase();
		if(fc.equals("YEARLY") || fc.equals("YEAR")) return Frequency.YEARLY;
		if(fc.equals("QUARTERLY") || fc.equals("QUARTER")) return Frequency.QUARTERLY;
		if(fc.equals("MONTHLY") || fc.equals("MONTH")) return Frequency.MONTHLY;
		if(fc.equals("WEEKLY") || fc.equals("WEEK")) return Frequency.WEEKLY;
		if(fc.equals("DAILY") || fc.equals("DAY")) return Frequency.DAILY;
		if(fc.equals("HOURLY") || fc.equals("HOUR")) return Frequency.HOURLY;
		throw new UnknownFreqCodeException("unknown freq code: "+fc);
	}

	public static String freqToFreqCode(int freq)
	  throws UnknownFreqCodeException {
		if(freq==Frequency.YEARLY) return "YEARLY";
		if(freq==Frequency.QUARTERLY) return "QUARTERLY";
		if(freq==Frequency.MONTHLY) return "MONTHLY";
		if(freq==Frequency.WEEKLY) return "WEEKLY";
		if(freq==Frequency.DAILY) return "DAILY";
		if(freq==Frequency.HOURLY) return "HOURLY";
		throw new UnknownFreqCodeException("unknown freq: "+freq);
	}

}
