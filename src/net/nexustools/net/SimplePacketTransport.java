/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net;

import java.io.IOException;
import net.nexustools.io.DataInputStream;
import net.nexustools.io.DataOutputStream;

/**
 *
 * @author katelyn
 */
public abstract class SimplePacketTransport<P extends Packet> implements PacketTransport<P> {

	public void lock() {}
	public void register(Class<? extends P> packetClass) throws NoSuchMethodException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int idFor(Packet packet) throws NoSuchMethodException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public int idFor(Class<? extends P> packetClass) throws NoSuchMethodException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public P read(DataInputStream inStream, Client client) throws IOException {
		P packet = create(0);
		readPayload(inStream, client, packet);
		return packet;
	}

	public void write(DataOutputStream outStream, Client client, Packet packet) throws IOException, NoSuchMethodException {
		writePayload(outStream, client, packet);
	}

	public void writePayload(DataOutputStream outStream, Client client, Packet packet) throws UnsupportedOperationException, IOException {
		packet.write(outStream, client);
	}

	public void readPayload(DataInputStream inStream, Client client, Packet packet) throws UnsupportedOperationException, IOException {
		packet.read(inStream, client);
	}

	public abstract P create(int id);
	
}
