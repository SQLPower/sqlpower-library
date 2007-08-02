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

	/**
	 * Replaces double quotes, ampersands, and less-than signs with
	 * their character reference equivalents.  This makes the returned
	 * string be safe for use as an XML content data or as an attribute value
	 * enclosed in double quotes. From the XML Spec at http://www.w3.org/TR/REC-xml/#sec-predefined-ent:
	 * 4.6 Predefined Entities
	 * "Definition: Entity and character references may both be used to escape the left angle bracket, ampersand,
	 * and other delimiters. A set of general entities (amp, lt, gt, apos, quot) is specified for this purpose...]
	 * All XML processors must recognize these entities whether they are declared or not. For interoperability,
	 * valid XML documents should declare these entities..."
	 */
	public static String escapeXML(String src) {
	    if (src == null) return "";
		StringBuffer sb = new StringBuffer(src.length()+10);  // arbitrary amount of extra space
		char ch;
	    
		for (int i = 0, n = src.length(); i < n; i++) {
			ch = src.charAt(i);
	        
	        if (ch == '\'') {
				sb.append("&apos;");
	        }
	        else if (ch == '"') {
				sb.append("&quot;");
	        }
	        else if (ch == '&') {
				sb.append("&amp;");
	        }
	        else if (ch == '<') {
				sb.append("&lt;");
	        }
	        else if (ch == '>') {
				sb.append("&gt;");
	        }
	        else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}
}