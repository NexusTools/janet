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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import net.nexustools.concurrent.PropList;
import net.nexustools.concurrent.ListAccessor;
import net.nexustools.concurrent.logic.VoidReader;
import net.nexustools.runtime.ThreadedRunQueue;
import net.nexustools.runtime.RunQueue;
import net.nexustools.utils.log.Logger;
import net.nexustools.utils.Testable;


/**
 *
 * @author kate
 */
public class Server<P extends Packet, C extends Client<P, ? extends Server>> extends Thread {
	
	public static enum Transport {
		TCP,
		UDP
	}
	
	public static ServerSocket spawn(int port, Transport protocol) throws IOException {
		switch(protocol) {
			case TCP:
				return new ServerSocket(port);
		}
		throw new UnsupportedOperationException();
	}
	
	final RunQueue runQueue;
	protected final PacketTransport<P> packetRegistry;
	final PropList<C> clients = new PropList<C>();
	final ServerSocket streamServer;
	
	public Server(int port, Transport protocol) throws IOException {
		this(spawn(port, protocol), new DefaultPacketTransport<P>());
	}
	public Server(int port, Transport protocol, RunQueue runQueue) throws IOException {
		this(spawn(port, protocol), new DefaultPacketTransport<P>(), runQueue);
	}
	public Server(int port, Transport protocol, PacketTransport<P> packetRegistry) throws IOException {
		this(spawn(port, protocol), packetRegistry);
	}
	public Server(int port, Transport protocol, PacketTransport<P> packetRegistry, RunQueue runQueue) throws IOException {
		this(spawn(port, protocol), packetRegistry, runQueue);
	}
	public Server(ServerSocket streamServer, PacketTransport<P> packetRegistry) {
		this(streamServer, packetRegistry, new ThreadedRunQueue("TCP" + streamServer.getLocalPort(), 1.5f));
	}
	protected Server(ServerSocket streamServer, PacketTransport<P> packetRegistry, RunQueue runQueue) {
		super("TCP" + streamServer.getLocalPort() + "Accept");
		this.packetRegistry = packetRegistry;
		this.streamServer = streamServer;
		this.runQueue = runQueue;
		start();
	}
	
	public RunQueue runQueue() {
		return runQueue;
	}
	
	public C createClient(Socket socket) throws IOException {
		return (C) new Client(socket, this);
	}
	
	public void send(final P packet, final Testable<C> shouldSend) {
		clients.read(new VoidReader<ListAccessor<C>>() {
			@Override
			public void readV(ListAccessor<C> data) {
				for(C client : data)
					if(shouldSend.test(client))
						client.send(packet);
			}
		});
	}
	public void sendAll(P packet) {
		send(packet, Testable.TRUE);
	}

	@Override
	public void run() {
		try {
			while(true) {
				C client = createClient(streamServer.accept());
				Logger.debug("Client Connected", client);
				// Dispatch connect event
				clients.push(client);
			}
		} catch(IOException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return getName();
	}
	
}