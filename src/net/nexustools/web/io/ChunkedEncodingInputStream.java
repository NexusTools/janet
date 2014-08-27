/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web.io;

import java.io.IOException;
import java.io.InputStream;
import net.nexustools.io.ProcessingInputStream;
import net.nexustools.utils.StringUtils;

/**
 *
 * @author katelyn
 */
public class ChunkedEncodingInputStream extends ProcessingInputStream {
	public final static byte[] LR = "\r\n".getBytes(StringUtils.UTF8);
	public final static byte[] ChunkEnd = "0\r\n\r\n".getBytes(StringUtils.UTF8);
	
	public ChunkedEncodingInputStream(InputStream underlying) {
		super(underlying, 0);
	}
	
	boolean endOfStream = false;
	
	@Override
	public int read(byte[] b, int off, int len, InputStream underlying, byte[]... buffers) throws IOException {
		if(endOfStream)
			return -1;

		int read = Math.min(buffers[0].length, len-20);
		read = underlying.read(buffers[0], 0, read);
		if(read < 1) {
			if(read < 0) {
				System.arraycopy(ChunkEnd, 0, b, off, ChunkEnd.length);
				endOfStream = true;
				return ChunkEnd.length;
			}
			return read;
		}

		byte[] chunkBytes = (Integer.toHexString(read) + "\r\n").getBytes(StringUtils.UTF8);
		System.arraycopy(chunkBytes, 0, b, off, chunkBytes.length);
		System.arraycopy(buffers[0], 0, b, off+chunkBytes.length, read);
		System.arraycopy(LR, 0, b, off+chunkBytes.length+read, LR.length);
		return read + chunkBytes.length + LR.length;
	}
	
}
