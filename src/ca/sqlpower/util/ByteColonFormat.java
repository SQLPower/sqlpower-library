package ca.sqlpower.util;

import java.text.*;
import java.util.*;

public class ByteColonFormat extends Format {

    public StringBuffer format(Object obj,
			       StringBuffer toAppendTo,
			       FieldPosition pos) {
	byte[] inputBytes=(byte[])obj;
	char[] hexDigits=new char[2];

	int i=0;
	while(true) {
	    toAppendTo.append(byteToChars(inputBytes[i], hexDigits));
	    if(i == (inputBytes.length)-1) {
		break;
	    }
	    toAppendTo.append(':');
	    i++;
	}
	return toAppendTo;
    }
    
    public byte[] parse(String source) {
	return (byte[])parseObject(source, new ParsePosition(0));
    }

    public Object parseObject (String source, ParsePosition pos) {
	String subsource = source.substring(pos.getIndex());
	StringTokenizer st = new StringTokenizer(subsource, ":");
	byte[] outputBytes = new byte[st.countTokens()];

	try {
	    for(int i=0; i<outputBytes.length; i++) {
		String thisToken=st.nextToken();
		outputBytes[i]=stringToByte(thisToken);
	    }

	    pos.setIndex(source.length());
	} catch(ParseException e) {
	    // do nothing; leaving pos unchanged indicates an error
	}

	// ASSERT: st.hasMoreElements() == false

	return outputBytes;
    }

    static char[] byteToChars(byte in, char[] out) {
	final String hexDigits="0123456789abcdef";
	out[0]=hexDigits.charAt((in & 0xf0) / 0x10);
	out[1]=hexDigits.charAt(in & 0x0f);
	return out;
    }

    static byte stringToByte(String in) throws ParseException {
	final String hexDigits="0123456789abcdef";
	byte out=0;

	if(in.length() == 1) {
	    out = (byte)hexDigits.indexOf(in.charAt(0));
	} else if(in.length() == 2) {
	    out |= (byte)(hexDigits.indexOf(in.charAt(0)) * 0x10);
	    out |= (byte)hexDigits.indexOf(in.charAt(1));
	} else {
	    throw new ParseException("Encountered a hex string that was not of length 1 or 2", 0);
	}

	return out;
    }
}
