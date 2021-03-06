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

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.net.SocketException;
import net.nexustools.concurrent.Condition;
import net.nexustools.concurrent.DefaultPropMap;
import net.nexustools.concurrent.Prop;
import net.nexustools.concurrent.PropList;
import net.nexustools.concurrent.logic.Writer;
import net.nexustools.data.accessor.ListAccessor;
import net.nexustools.event.DefaultEventDispatcher;
import net.nexustools.event.EventDispatcher;
import net.nexustools.io.DataInputStream;
import net.nexustools.io.DataOutputStream;
import net.nexustools.janet.ClientListener.ClientEvent;
import net.nexustools.janet.Server.Protocol;
import net.nexustools.runtime.FairTaskDelegator.FairRunnable;
import net.nexustools.runtime.RunQueue;
import net.nexustools.runtime.ThreadedRunQueue;
import net.nexustools.utils.Creator;
import net.nexustools.utils.NXUtils;
import net.nexustools.utils.Pair;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author kate
 */
public class Client<P extends Packet, S extends Server<P, ?>> {
	
	private static final ThreadedRunQueue sendQueue = new ThreadedRunQueue("ClientOut", ThreadedRunQueue.Delegator.Fair, 2f);
	static final DefaultPropMap<Class<?>, Prop<?>> clientStorage = new DefaultPropMap<Class<?>, Prop<?>>(new Creator<Prop<?>, Class<?>>() {
		public Prop<?> create(Class<?> using) {
			return new Prop();
		}
	});
	
	static final ThreadLocal<Client> currentClient = new ThreadLocal();

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
				currentClient.set(Client.this);
				while(true) {
					final P packet = nextPacket();
					if(packet == null)
						throw new EOFException();

					Logger.gears("Received Packet", packet);
					runQueue.push(packetProcessor(packet));
				}
			} catch (EOFException ex) {
				Logger.exception(Logger.Level.Gears, ex);
			} catch (SocketException ex) {
				Logger.exception(Logger.Level.Gears, ex);
			} catch (IOException ex) {
				Logger.exception(ex);
			} finally {
				isAlive.set(false);
				Logger.debug("Client Disconnected", Client.this);
				try {
					socketImpl.close();
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
						writeQueue(); // Dump remaining packets
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
		
		throw new UnsupportedOperationException();
	}
	
	protected boolean killAfter;
	protected final RunQueue runQueue;
	protected final Closeable socketImpl;
	final PacketTransport<P> packetRegistry;
	final Condition shutdown = new Condition();
	final Prop<Boolean> isAlive = new Prop(true);
	final PropList<P> packetQueue = new PropList();
	protected final Pair<DataInputStream,DataOutputStream> socket;
	final DefaultEventDispatcher<?, ClientListener, ClientListener.ClientEvent> eventDispatcher;
	final DefaultEventDispatcher<?, PacketListener, PacketListener.PacketEvent> packetDispatcher;
	final ReceiveThread receiveThread;
	final Runnable processSendQueue;
	final Server server;
	public Client(final Socket socket, final Server server) throws IOException {
		final int clientHash = socket.getInetAddress().toString().hashCode();
		receiveThread = new ReceiveThread(getClass().getSimpleName()) {
			@Override
			public Runnable packetProcessor(final P packet) {
				return new FairRunnable() {
					public void run() {
						currentClient.set(Client.this);
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
				writeQueue();
				if(killAfter) {
					writeQueue();
					try {
						socket.close();
					} catch (IOException ex) {}
				}
			}
			public int fairHashCode() {
				return clientHash;
			}
		};
		
		eventDispatcher = new DefaultEventDispatcher(server.runQueue);
		packetDispatcher = new DefaultEventDispatcher(server.runQueue);
		this.socket = new Pair(new DataInputStream(socket.getInputStream()) {
			@Override
			public void close() throws IOException {
				super.close();
				socketImpl.close();
			}
		}, new DataOutputStream(socket.getOutputStream()) {
			@Override
			public void close() throws IOException {
				super.close();
				socketImpl.close();
			}
		});
		this.packetRegistry = server.packetRegistry;
		this.runQueue = server.runQueue;
		this.server = server;
		
		socketImpl = socket;
		receiveThread.start();
	}
	public Client(final Socket socket, RunQueue runQueue, PacketTransport packetRegistry) throws IOException {
		final int clientHash = socket.getInetAddress().toString().hashCode();
		receiveThread = new ReceiveThread(getClass().getSimpleName()) {
			@Override
			public Runnable packetProcessor(final P packet) {
				return new FairRunnable() {
					public void run() {
						currentClient.set(Client.this);
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
				writeQueue();
				if(killAfter) {
					writeQueue();
					try {
						socket.close();
					} catch (IOException ex) {}
				}
			}
			public int fairHashCode() {
				return clientHash;
			}
		};
		
		this.runQueue = runQueue;
		eventDispatcher = new DefaultEventDispatcher(runQueue);
		packetDispatcher = new DefaultEventDispatcher(runQueue);
		this.socket = new Pair(new DataInputStream(socket.getInputStream()) {
			@Override
			public void close() throws IOException {
				super.close();
				socketImpl.close();
			}
		}, new DataOutputStream(socket.getOutputStream()) {
			@Override
			public void close() throws IOException {
				super.close();
				socketImpl.close();
			}
		});
		this.packetRegistry = packetRegistry;
		this.server = null;
		
		socketImpl = socket;
		receiveThread.start();
	}
	public Client(String host, int port, Protocol protocol, RunQueue runQueue, PacketTransport packetRegistry) throws IOException {
		this(open(host, port, protocol), runQueue, packetRegistry);
	}
	public Client(String name, String host, int port, Protocol protocol, PacketTransport packetRegistry) throws IOException {
		this(host, port, protocol, new ThreadedRunQueue(name), packetRegistry);
	}
	
	public Server server() {
		return server;
	}
	
	protected void writeQueue() {
		currentClient.set(this);
		ListAccessor<P> packets = packetQueue.take();
		Logger.gears("Writing", packets.length(), "packets", this);
		for(final P packet : packets)
			shutdown.ifRun(new Runnable() {
				public void run() {
					packet.failedToComplete(Client.this, new EOFException());
				}
			}, new Runnable() {
				public void run() {
					try {
						packet.aboutToSend(Client.this);
						writePacket(packet);
						packet.sendComplete(Client.this);
					} catch (IOException t) {
						packet.failedToComplete(Client.this, t);
						Logger.exception(Logger.Level.Gears, t);
					} catch (Throwable t) {
						packet.failedToComplete(Client.this, t);
						Logger.exception(Logger.Level.Error, t);
					}
				}
			});
	}
	
	protected void writePacket(P packet) throws IOException, NoSuchMethodException {
		packetRegistry.write(socket.v, Client.this, packet);
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
		if(killAfter || shutdown.isFinished())
			packet.failedToComplete(Client.this, new EOFException());
		else
			try {
				packetQueue.write(new Writer<ListAccessor<P>>() {
					@Override
					public void write(ListAccessor<P> data) {
						data.push(packet);
						sendQueue.push(processSendQueue, RunQueue.Placement.ReplaceExisting);
					}
				});
			} catch (InvocationTargetException ex) {
				throw NXUtils.wrapRuntime(ex);
			}
	}

	public boolean isConnected() {
		return isAlive.get();
	}
	
	public void sendAndKill(final P packet) {
		if(killAfter || shutdown.isFinished())
			packet.failedToComplete(Client.this, new EOFException());
		else
			try {
				packetQueue.write(new Writer<ListAccessor<P>>() {
					@Override
					public void write(ListAccessor<P> data) {
						killAfter = true;
						data.push(packet);
						sendQueue.push(processSendQueue, RunQueue.Placement.ReplaceExisting);
					}
				});
			} catch (InvocationTargetException ex) {
				throw NXUtils.wrapRuntime(ex);
			}
	}
	public void kill() {
		try {
			socketImpl.close();
		} catch (IOException ex) {}
	}

}
