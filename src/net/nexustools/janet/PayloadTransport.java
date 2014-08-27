/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.janet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import net.nexustools.io.DataInputStream;
import net.nexustools.io.DataOutputStream;
import net.nexustools.io.MemoryStream;

/**
 *
 * @author kate
 */
public class PayloadTransport extends DefaultPacketTransport {

	@Override
	public void writePayload(DataOutputStream outStream, Client client, Packet packet) throws UnsupportedOperationException, IOException {
		MemoryStream inStream = new MemoryStream();
		super.writePayload(inStream.createDataOutputStream(), client, packet);
		outStream.writeShort((int) inStream.size());
		outStream.write(inStream.toByteArray());
	}

	@Override
	public void readPayload(DataInputStream inStream, Client client, Packet packet) throws UnsupportedOperationException, IOException {
		super.readPayload(new MemoryStream(inStream, inStream.readShort()).createDataInputStream(), client, packet);
	}
	
}
