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

import java.net.Socket;
import java.io.IOException;
import net.nexustools.DefaultAppDelegate;
import net.nexustools.janet.Server.Protocol;
import net.nexustools.runtime.ThreadedRunQueue;
import net.nexustools.utils.log.Logger;
import net.nexustools.concurrent.Prop;

/**
 *
 * @author kate
 */
public abstract class ServerAppDelegate<C extends Client, S extends Server> extends DefaultAppDelegate {

    private Prop<Runnable> mainLoop = new Prop();
    protected final PacketTransport packetRegistry;

    public ServerAppDelegate(String[] args, String name, String organization, PacketTransport packetRegistry, float multiplier) {
        super(args, name, organization, new ThreadedRunQueue(name, ThreadedRunQueue.Delegator.Fair, multiplier));
        this.packetRegistry = packetRegistry;
    }

    public ServerAppDelegate(String[] args, String name, String organization, float multiplier) {
        this(args, name, organization, new DefaultPacketTransport(), multiplier);
    }

    public ServerAppDelegate(String[] args, String name, String organization) {
        this(args, name, organization, 1.5f);
    }

    protected abstract void populate(PacketTransport registry) throws NoSuchMethodException;

    protected C createClient(String host, int port) throws IOException {
        return (C) new Client(name + "Client", host, port, Protocol.TCP, packetRegistry);
    }

    protected C createClient(Socket socket, S server) throws IOException {
        return (C) new Client(socket, server);
    }

    protected S createServer(int port) throws IOException {
        return (S) new Server(port, Protocol.TCP, packetRegistry, runQueue);
    }

    protected abstract void launchClient(C client);

    protected void launchServer(S server) {}

    protected Runnable launch(String[] args) throws NoSuchMethodException, IOException {
		int id = 0;
		Logger.quote(Logger.Level.Debug, "Poluating Packet Registry for", this);
		populate(packetRegistry);
		packetRegistry.lock();

		if (args.length == 2) {
			Logger.gears("Creating Client", args);
			final C client = createClient(args[0], Integer.valueOf(args[1]));
			Logger.gears("Installing Client MainLoop", args);
			Logger.debug("Launching Client", client);
			launchClient(client);
			
			return new Runnable() {
				public void run() {
					Logger.gears("Waiting for Client to Disconnect", client);
					client.shutdown.waitFor();
				}
			};
		} else if (args.length == 1) {
			Logger.gears("Creating Server", args);
			final S server = createServer(Integer.valueOf(args[0]));
			Logger.gears("Installing Server MainLoop", args);
			Logger.debug("Launching Server", server);
			launchServer(server);
			
			return new Runnable() {
				public void run() {
					while (server.isAlive())
						try {
							server.join();
						} catch (InterruptedException ex) {}
				}
			};
		} else {
			throw new UnsupportedOperationException("Required 1 or 2 arguments, (HOST PORT) or (PORT)");
		}
    }

    public boolean needsMainLoop() {
        return false;
    }

}
