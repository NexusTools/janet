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

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import net.nexustools.concurrent.PropList;
import net.nexustools.concurrent.logic.VoidReader;
import net.nexustools.data.accessor.ListAccessor;
import net.nexustools.io.Stream;
import net.nexustools.runtime.RunQueue;
import net.nexustools.runtime.ThreadedRunQueue;
import net.nexustools.utils.NXUtils;
import net.nexustools.utils.Testable;
import net.nexustools.utils.log.Logger;


/**
 *
 * @author kate
 */
public class Server<P extends Packet, C extends Client<P, ? extends Server>> extends Thread {
	
	public static enum Protocol {
		TCP,
		SSLvTCP,
		UDP
	}
	
	public static ServerSocket spawn(int port, Protocol protocol, Object... args) throws IOException {
		switch(protocol) {
			case TCP:
				return new ServerSocket(port);
				
			case SSLvTCP:
				try {
					if(args.length < 2)
						throw new IllegalArgumentException("SSLvTCP requires a key path and key password.");
					
					// init keystore
					KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
					keyStore.load(Stream.openInputStream((String) args[0]), (char[]) args[1]);
					// init KeyManagerFactory
					KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
					keyManagerFactory.init(keyStore, (char[]) args[1]);
					// init KeyManager
					KeyManager keyManagers[] = keyManagerFactory.getKeyManagers();
					// init the SSL context
					SSLContext sslContext = SSLContext.getDefault();
					sslContext.init(keyManagers, null, new SecureRandom());
					// get the socket factory
					SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();

					// and finally, get the socket
					return socketFactory.createServerSocket(port);
				} catch (KeyStoreException ex) {
					throw NXUtils.unwrapIOException(ex);
				} catch (NoSuchAlgorithmException ex) {
					throw NXUtils.unwrapIOException(ex);
				} catch (CertificateException ex) {
					throw NXUtils.unwrapIOException(ex);
				} catch (UnrecoverableKeyException ex) {
					throw NXUtils.unwrapIOException(ex);
				} catch (KeyManagementException ex) {
					throw NXUtils.unwrapIOException(ex);
				} catch (URISyntaxException ex) {
					throw NXUtils.unwrapIOException(ex);
				}
		}
		throw new UnsupportedOperationException();
	}
	
	final RunQueue runQueue;
	protected final PacketTransport<P> packetRegistry;
	final PropList<C> clients = new PropList<C>();
	final ServerSocket streamServer;
	
	public Server(int port, Protocol protocol, Object... args) throws IOException {
		this(spawn(port, protocol, args), new DefaultPacketTransport<P>());
	}
	public Server(int port, Protocol protocol, RunQueue runQueue, Object... args) throws IOException {
		this(spawn(port, protocol, args), new DefaultPacketTransport<P>(), runQueue);
	}
	public Server(int port, Protocol protocol, PacketTransport<P> packetRegistry, Object... args) throws IOException {
		this(spawn(port, protocol, args), packetRegistry);
	}
	public Server(int port, Protocol protocol, PacketTransport<P> packetRegistry, RunQueue runQueue, Object... args) throws IOException {
		this(spawn(port, protocol, args), packetRegistry, runQueue);
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
		try {
			clients.read(new VoidReader<ListAccessor<C>>() {
				@Override
				public void readV(ListAccessor<C> data) throws Throwable {
					for(C client : data)
						if(shouldSend.test(client))
							client.send(packet);
				}
			});
		} catch (InvocationTargetException ex) {
			throw NXUtils.unwrapRuntime(ex);
		}
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
