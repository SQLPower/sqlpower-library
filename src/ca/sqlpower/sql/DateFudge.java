package ca.sqlpower.sql;

import java.util.*;

/**
 * The DateFudge class helps to get around a sticky timezone problem
 * in Oracle. It shouldn't have to exist!
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class DateFudge {
    long offsetInMillis;

    /**
     * Creates a new Date Fudge object, set to a specific offset.
     *
     * @param gmtOffset the offset in minutes.  For Eastern time, this
     * would be -300 (5 hours later than GMT).
     */
    public DateFudge(int gmtOffset) {
	offsetInMillis=gmtOffset*60*1000;
    }

    /**
     * Sets the given Date ahead or back by the offset specified when
     * this DateFudge object was created.
     */
    public void fudge(Date date) {
	date.setTime(date.getTime()+offsetInMillis);
    }
}
