package ca.sqlpower.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Represents an IPv4 network, which we think of as an IP address
 * together with a netmask.
 */
public class IPNetwork {

	protected InetAddress addr;
	protected InetAddress mask;

	/**
	 * 
	 * @param addr A dotted-quad address or a DNS name such as
	 * "192.168.0.0" or "lan.sqlpower.ca".
	 * @param mask A dotted-quad mask such as "255.255.0.0"
	 */
	public IPNetwork(String addr, String mask) throws UnknownHostException {
		this.addr = InetAddress.getByName(addr);
		this.mask = InetAddress.getByName(mask);
	}

	/**
	 *
	 * @param addrMask An ip/netmask string such as "192.168.0.0/24".
	 */
	public IPNetwork(String addrMask)
		throws IllegalArgumentException, NumberFormatException, UnknownHostException {
		int slashIdx = addrMask.indexOf("/");
		if (slashIdx < 0) {
			throw new IllegalArgumentException("Couldn't find '/' in argument string.");
		}
		String addrString = addrMask.substring(0,slashIdx);
		System.out.println("Address part: "+addrString);
		this.addr = InetAddress.getByName(addrString);

		String maskString = addrMask.substring(slashIdx+1, addrMask.length());
		System.out.println("Mask part: "+maskString);
		int maskInt = Integer.parseInt(maskString);

		int[] myMask = new int[4];
		for (int i = 0; i < 4; i++) {
			if (maskInt <= 0) {
				myMask[i] = 0;
			} else if (maskInt == 1) {
				myMask[i] = 128;
			} else if (maskInt == 2) {
				myMask[i] = 128+64;
			} else if (maskInt == 3) {
				myMask[i] = 128+64+32;
			} else if (maskInt == 4) {
				myMask[i] = 128+64+32+16;
			} else if (maskInt == 5) {
				myMask[i] = 128+64+32+16+8;
			} else if (maskInt == 6) {
				myMask[i] = 128+64+32+16+8+4;
			} else if (maskInt == 7) {
				myMask[i] = 128+64+32+16+8+4+2;
			} else if (maskInt >= 8) {
				myMask[i] = 128+64+32+16+8+4+2+1;
			}
			maskInt -= 8;
		}
		this.mask = InetAddress.getByName(myMask[0]+"."+myMask[1]+"."+myMask[2]+"."+myMask[3]);
	}

	public String toString() {
		return addr+" mask "+mask;
	}

	/**
	 * Checks if the given InetAddress is a part of this IPNetwork.
	 */
	public boolean contains(InetAddress checkMe) {
		for (int i = 0; i < 4; i++) {
			if ( (addr.getAddress()[i] & mask.getAddress()[i]) != 
				 (checkMe.getAddress()[i] & mask.getAddress()[i]) ) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if the significant portions of this IPNetwork and the
	 * given IPNetwork are the same.
	 */
	public boolean equals(Object other) {
		IPNetwork otherNet = (IPNetwork) other;
		byte[] thisAddr = addr.getAddress();
		byte[] thisMask = mask.getAddress();
		byte[] thatAddr = otherNet.addr.getAddress();
		byte[] thatMask = otherNet.mask.getAddress();

		for (int i = 0; i < 4; i++) {
			if ((thisAddr[i] & thisMask[i]) != (thatAddr[i] & thatMask[i])) {
				return false;
			}
		}
		return true;
	}
}
