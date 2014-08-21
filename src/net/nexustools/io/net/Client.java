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

package net.nexustools.io.net;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import javax.activation.UnsupportedDataTypeException;
import net.nexustools.concurrent.Condition;
import net.nexustools.concurrent.ListAccessor;
import net.nexustools.concurrent.Prop;
import net.nexustools.concurrent.PropList;
import net.nexustools.concurrent.logic.Writer;
import net.nexustools.event.DefaultEventDispatcher;
import net.nexustools.event.EventDispatcher;
import net.nexustools.io.DataInputStream;
import net.nexustools.io.DataOutputStream;
import net.nexustools.io.net.ClientListener.ClientEvent;
import net.nexustools.io.net.Server.Protocol;
import net.nexustools.runtime.FairTaskDelegator.FairRunnable;
import net.nexustools.runtime.RunQueue;
import net.nexustools.runtime.ThreadedRunQueue;
import net.nexustools.utils.Pair;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author kate
 */
public class Client<P extends Packet, S extends Server<P, ?>> {
	
	private static final ThreadedRunQueue sendQueue = new ThreadedRunQueue("ClientOut", ThreadedRunQueue.Delegator.Fair, 2f);

	private abstract class ReceiveThread extends Thread {
		public ReceiveThread(String name) {
			super(name + "In");
			setDaemon(true);
		}
		public abstract Runnable packetProcessor(P packet);
		public abstract Object eventSource();
		@Override
		public void run() {
			try {
				while(true) {
					final P packet = nextPacket();
					if(packet == null)
						throw new IOException("Unexpected end of stream");

					Logger.gears("Received Packet", packet);
					runQueue.push(packetProcessor(packet));
				}
			} catch (DisconnectedException ex) {
				Logger.exception(Logger.Level.Gears, ex);
			} catch (IOException ex) {
				Logger.exception(ex);
			} finally {
				isAlive.set(false);
				Logger.debug("Client Disconnected", Client.this);
				try {
					socket.i.close();
				} catch (IOException ex) {}

				shutdown.finish(new Runnable() {
					public void run() {
						eventDispatcher.dispatch(new EventDispatcher.Processor<ClientListener, ClientListener.ClientEvent>() {
							public ClientListener.ClientEvent create() {
								return new ClientListener.ClientEvent(eventSource(), Client.this);
							}
							public void dispatch(ClientListener listener, ClientListener.ClientEvent event) {
								listener.clientDisconnected(event);
							}
						});
						writeOut(); // Dump remaining packets
					}
				});
			}
			
		}
	}
	
	public static Socket open(String host, int port, Protocol protocol) throws IOException {
		switch(protocol) {
			case TCP:
				Logger.gears("Opening TCP Socket", host, port);
				return new Socket(host, port);
		}
		
		throw new UnsupportedDataTypeException();
	}
	
	protected final RunQueue runQueue;
	final PacketRegistry<P> packetRegistry;
	final Condition shutdown = new Condition();
	final Prop<Boolean> isAlive = new Prop(true);
	final PropList<P> packetQueue = new PropList();
	protected final Pair<DataInputStream,DataOutputStream> socket;
	final DefaultEventDispatcher<?, ClientListener, ClientListener.ClientEvent> eventDispatcher;
	final DefaultEventDispatcher<?, PacketListener, PacketListener.PacketEvent> packetDispatcher;
	final ReceiveThread receiveThread;
	final Runnable processSendQueue;
	final Server server;
	public Client(String name, final Socket socket, final Server server) throws IOException {
		final int clientHash = socket.getInetAddress().toString().hashCode();
		receiveThread = new ReceiveThread(name) {
			@Override
			public Runnable packetProcessor(final P packet) {
				return new FairRunnable() {
					public void run() {
						packet.recvFromClient(Client.this, server);
					}
					public int fairHashCode() {
						return clientHash;
					}
				};
			}
			@Override
			public Object eventSource() {
				return server;
			}
		};
		Logger.debug(socket.getInetAddress().toString());
		processSendQueue = new FairRunnable() {
			public void run() {
				writeOut();
			}
			public int fairHashCode() {
				return clientHash;
			}
		};
		
		eventDispatcher = new DefaultEventDispatcher(server.runQueue);
		packetDispatcher = new DefaultEventDispatcher(server.runQueue);
		this.socket = new Pair(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
		this.packetRegistry = server.packetRegistry;
		this.runQueue = server.runQueue;
		this.server = server;
		
		receiveThread.start();
	}
	public Client(String name, final Socket socket, RunQueue runQueue, PacketRegistry packetRegistry) throws IOException {
		final int clientHash = socket.getInetAddress().toString().hashCode();
		receiveThread = new ReceiveThread(name) {
			@Override
			public Runnable packetProcessor(final P packet) {
				return new FairRunnable() {
					public void run() {
						packet.recvFromServer(Client.this);
					}
					public int fairHashCode() {
						return clientHash;
					}
				};
			}
			@Override
			public Object eventSource() {
				return Client.this;
			}
		};
		processSendQueue = new FairRunnable() {
			public void run() {
				writeOut();
			}
			public int fairHashCode() {
				return clientHash;
			}
		};
		
		this.runQueue = runQueue;
		eventDispatcher = new DefaultEventDispatcher(runQueue);
		packetDispatcher = new DefaultEventDispatcher(runQueue);
		this.socket = new Pair(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
		this.packetRegistry = packetRegistry;
		this.server = null;
		
		receiveThread.start();
	}
	public Client(String name, String host, int port, Protocol protocol, RunQueue runQueue, PacketRegistry packetRegistry) throws IOException {
		this(name, open(host, port, protocol), runQueue, packetRegistry);
	}
	public Client(String name, String host, int port, Protocol protocol, PacketRegistry packetRegistry) throws IOException {
		this(name, host, port, protocol, new ThreadedRunQueue(name), packetRegistry);
	}
	
	public Server server() {
		return server;
	}
	
	protected void writeOut() {
		List<P> packets = packetQueue.take();
		Logger.gears("Writing", packets.size(), "packets", this);
		for(final P packet : packets)
			shutdown.ifRun(new Runnable() {
				public void run() {
					packet.failedToSend(Client.this, new DisconnectedException());
				}
			}, new Runnable() {
				public void run() {
					try {
						packet.aboutToSend(Client.this);
						packetRegistry.write(socket.v, Client.this, packet);
						packet.sendComplete(Client.this);
					} catch (Throwable t) {
						packet.failedToSend(Client.this, t);
						Logger.exception(t);
					}
				}
			});
	}
	
	public final void addClientListener(final ClientListener listener) {
		shutdown.ifRun(new Runnable() {
			public void run() {
				listener.clientDisconnected(new ClientEvent(Client.this, Client.this));
			}
		}, new Runnable() {
			public void run() {
				eventDispatcher.add(listener);
			}
		});
		
	}
	
	public final void removeClientListener(ClientListener listener) {
		eventDispatcher.remove(listener);
	}
	
	public final void addPacketListener(PacketListener listener) {
		packetDispatcher.add(listener);
	}
	
	public final void removePacketListener(PacketListener listener) {
		packetDispatcher.remove(listener);
	}
	
	public P nextPacket() throws IOException {
		return (P) packetRegistry.read(socket.i, this);
	}
	
	public void send(final P packet) {
		if(shutdown.isFinished())
			packet.failedToSend(Client.this, new DisconnectedException());
		else
			packetQueue.write(new Writer<ListAccessor<P>>() {
				@Override
				public void write(ListAccessor<P> data) {
					data.push(packet);
					sendQueue.push(processSendQueue, RunQueue.Placement.ReplaceExisting);
				}
			});
	}

	public boolean isConnected() {
		return isAlive.get();
	}

}
