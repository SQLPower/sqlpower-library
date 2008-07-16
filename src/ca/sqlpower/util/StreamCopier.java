/*
 * Copyright (c) 2008, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
	
	/**
	 * Creates a new StreamCopier that monitors the given input stream and
	 * collects its data.
	 * <p>
	 * Remember to start this copier with the start() method if you want it
	 * to run in the background.
	 * 
	 * @param in
	 *            The stream to read. It will be read to EOF, but not closed.
	 * @param out
	 *            The stream to write to. It will be closed when EOF is
	 *            encountered on the input stream.
	 */
	public StreamCopier(InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
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
				out.close();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public synchronized boolean eofEncountered() {
		return eof;
	}
}
