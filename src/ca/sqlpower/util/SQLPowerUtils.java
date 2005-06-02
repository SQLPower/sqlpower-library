package ca.sqlpower.util;

public class SQLPowerUtils {
	
	public static boolean areEqual (Object o1, Object o2) {
		if (o1 == o2) {
			// this also covers (null == null)
			return true;
		} else {
			if (o1 == null || o2 == null) {
				return false;
			} else {
				// pass through to object equals method
				return o1.equals(o2);
			}
		}
	}
}