package ca.sqlpower.util.test;

import java.io.*;

class ZealousURLEncoderTest {
    public static void main(String[] args) throws Exception {
	String first;
	try {
	    first=args[0];
	} catch (Exception e) {
	    first="This is a stock test string.  You can supply your own on the command-line!";
	}
	String second=ca.sqlpower.util.ZealousURLEncoder.zealousEncode(first);
	String third=java.net.URLDecoder.decode(second);

	System.out.println("       Your original string: "+first);
	System.out.println("       Your string, encoded: "+second);
	System.out.println("The encoded, decoded string: "+third);
    }
}
