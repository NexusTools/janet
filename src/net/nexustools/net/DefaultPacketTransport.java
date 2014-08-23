/*
 * janxutils is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 or any later version.
 * 
 * janxutils is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with janxutils.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package net.nexustools.net;

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import net.nexustools.io.DataInputStream;
import net.nexustools.io.DataOutputStream;
import net.nexustools.utils.Pair;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author kate
 */
public class DefaultPacketTransport<P extends Packet> implements PacketTransport<P> {
	
	private static enum Type {
		Byte,
		Short,
		Integer
	}
	
	static class Entry extends Pair<Constructor<?>, Class<?>> {
		
		public Entry(Constructor<?> constructor, Class<?> clazz) {
			super(constructor, clazz);
		}

		@Override
		public int hashCode() {
			return v.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			return ((Entry)obj).v == v;
		}
		
	}
	
	Type packetIDType;
	ArrayList<Entry> entryList = new ArrayList();
	Entry[] registered;
	
	public void register(Class<? extends P> packetClass) throws NoSuchMethodException {
		if(entryList.size() >= 0xFFFF)
			throw new RuntimeException("There is a limit of 0xFFFF packet types, to add more make SubPackets or override the nextPacket method with your own implementation.");
		entryList.add(new Entry(packetClass.getConstructor(), packetClass));
	}
	
	public int idFor(P packet) throws NoSuchMethodException {
		return idFor((Class<? extends P>)packet.getClass());
	}
	
	public int idFor(Class<? extends P> packetClass) throws NoSuchMethodException {
		int pos = 0;
		for(Entry entry : registered) {
			if(entry.v == packetClass)
				return pos;
			pos++;
		}
		return -1;
	}
	
	
	public P read(DataInputStream inStream, Client<P, ?> client) throws IOException {
		Logger.gears("Waiting for packet", this);
		int packetID;
		try {
			switch(packetIDType) {
				case Integer:
					packetID = inStream.readInt();
					break;

				case Short:
					packetID = inStream.readShort();
					break;

				case Byte:
					packetID = inStream.read();
					if(packetID < 0)
						throw new DisconnectedException();
					break;
					
				default:
					throw new UnsupportedOperationException();
			}
		} catch(EOFException eof) {
			throw new DisconnectedException(eof);
		}
		Logger.gears("Reading packet", packetID);
		P packet = create(packetID);
		try {
			readPayload(inStream, client, packet);
			Logger.gears("Read packet", packet);
		} catch (UnsupportedOperationException ex) {
			throw new IOException(ex);
		}
		return packet;
	}
	
	public void write(DataOutputStream outStream, Client<P, ?> client, P packet) throws IOException, NoSuchMethodException {
		int packetID = idFor(packet);
		Logger.gears("Writing Packet", packetID, packet);

		byte[] data;
		try {
			data = packet.data(client);
		} catch(Throwable t) {
			Logger.warn("Error generating packet contents");
			Logger.warn("Client may now become unstable");
			Logger.exception(Logger.Level.Warning, t);
			return;
		}

		switch(packetIDType) {
			case Integer:
				outStream.writeInt(packetID);
				break;
				
				
			case Short:
				outStream.writeShort(packetID);
				break;
				
				
			case Byte:
				outStream.write(packetID);
				break;
					
			default:
				throw new UnsupportedOperationException();
		}
		writePayload(outStream, client, packet);
		outStream.flush();
	}
	
	public void writePayload(DataOutputStream outStream, Client<P, ?> client, P packet) throws UnsupportedOperationException, IOException {
		packet.write(outStream, client);
	}
	
	public void readPayload(DataInputStream inStream, Client<P, ?> client, P packet) throws UnsupportedOperationException, IOException {
		packet.read(inStream, client);
	}
	
	public P create(int id) {
		try {
			return (P)registered[id].i.newInstance();
		} catch (InstantiationException ex) {
			throw new RuntimeException(ex);
		} catch (IllegalAccessException ex) {
			throw new RuntimeException(ex);
		} catch (IllegalArgumentException ex) {
			throw new RuntimeException(ex);
		} catch (InvocationTargetException ex) {
			throw new RuntimeException(ex);
		} catch (IndexOutOfBoundsException ex) {
			throw new RuntimeException("Packet ID not registered: " + id);
		}
	}
	
	public void lock() {
		registered = entryList.toArray(new Entry[entryList.size()]);
		entryList = null;
		
		if(registered.length > 0xFFFF)
			packetIDType = Type.Integer;
		else if(registered.length > 0xFF)
			packetIDType = Type.Short;
		else
			packetIDType = Type.Byte;
		
		int len = registered.length;
		Logger.quote(len + " Packets Registered for", this);
		if (len > 0xFFFF) {
			len = 8;
		} else if (len > 0xFF) {
			len = 4;
		} else {
			len = 2;
		}

		int id = 0;
		for (Entry entry : registered) {
			String idStr = Integer.toHexString(id++).toUpperCase();
			while (idStr.length() < len) {
				idStr = "0" + idStr;
			}
			Logger.quote(Logger.Level.Debug, "0x" + idStr, ((Class) entry.v).getName());
		}
	}
	
}
