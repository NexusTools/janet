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

package net.nexustools.janet;

import java.io.IOException;
import net.nexustools.io.DataInputStream;
import net.nexustools.io.DataOutputStream;

/**
 *
 * @author kate
 */
public interface PacketTransport<P extends Packet> {
	
	public void lock();
	public void register(Class<? extends P> packetClass);
	
	public int idFor(P packet) throws NoSuchMethodException;
	public int idFor(Class<? extends P> packetClass);
	
	public P read(DataInputStream inStream, Client<P, ?> client) throws IOException;
	public void write(DataOutputStream outStream, Client<P, ?> client, P packet) throws IOException;
	
	public void writePayload(DataOutputStream outStream, Client<P, ?> client, P packet) throws IOException;
	public void readPayload(DataInputStream inStream, Client<P, ?> client, P packet) throws IOException;
	
	public P create(int id);
	
}
