/*
 * Copyright (c) 2009, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The StreamCopier takes any InputStream and reads from it continuously,
 * copying each byte to a given output stream until it reaches end-of-file on
 * the input stream.
 */
public class StreamCopier extends Thread {
	
	/**
	 * The input stream whose bytes are being collected.
	 */
	private final InputStream in;

	/**
	 * The output stream where we write the bytes read from the input stream.
	 */
	private final OutputStream out;

	/**
	 * Keeps track of whether or now EOF has been encountered on the input stream.
	 */
	private boolean eof = false;
	
	private final boolean closeOutputStream;
	
	/**
	 * Creates a new StreamCopier that monitors the given input stream and
	 * collects its data.
	 * <p>
	 * Remember to start this copier with the start() method if you want it to
	 * run in the background.
	 * 
	 * @param in
	 *            The stream to read. It will be read to EOF, but not closed.
	 * @param out
	 *            The stream to write to.
	 * @param closeOutputStream
	 *            Indicates whether the output stream will be closed when EOF is
	 *            encountered on the input stream.
	 */
	public StreamCopier(InputStream in, OutputStream out, boolean closeOutputStream) {
		this.in = in;
		this.out = out;
		this.closeOutputStream = closeOutputStream;
	}

	/**
	 * Enters the main loop, stopping only when EOF is reached on the input stream
	 * or there is an IO Exception.  Remember that if you call this method directly,
	 * this will happen on the calling thread.  You probably want to call start()
	 * instead, which will invoke this run() method on a new thread.
	 */
	public void run() {
		try {
			int ch;
			while ((ch = in.read()) != -1) {
				synchronized (this) {
					out.write(ch);
				}
			}
			synchronized (this) {
				eof = true;
				if (closeOutputStream) {
					out.close();
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public synchronized boolean eofEncountered() {
		return eof;
	}
}
