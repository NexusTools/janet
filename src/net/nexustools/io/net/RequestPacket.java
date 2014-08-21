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

import java.util.Collection;
import net.nexustools.concurrent.DefaultPropMap;
import net.nexustools.concurrent.MapAccessor;
import net.nexustools.concurrent.PropMap;
import net.nexustools.concurrent.logic.Reader;
import net.nexustools.concurrent.logic.WriteReader;
import net.nexustools.concurrent.logic.Writer;
import net.nexustools.utils.Creator;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author kate
 */
public abstract class RequestPacket<R extends ResponsePacket, C extends Client, S extends Server> extends RefPacket<C, S> {

    protected static class ClientRequests {
        PropMap<Integer, RequestPacket> sent = new PropMap();
		final ClientListener clientListener;
		final Client client;
        int nextID = 0;
		
		private ClientRequests(final Client client) {
			clientListener = new ClientListener() {
				public void clientConnected(ClientListener.ClientEvent clientConnectedEvent) {}
				public void clientDisconnected(ClientListener.ClientEvent clientConnectedEvent) {
					client.removeClientListener(clientListener);
					Collection<RequestPacket> requests = sent.take().values();
					if(requests.size() > 0) {
						Logger.gears("Client Imploded", requests.size(), "Requests");
						for(RequestPacket packet : requests)
							packet.failedToComplete(client);
					}
				}
			};
			client.addClientListener(clientListener);
			this.client = client;
		}
		
		public int push(final RequestPacket request) {
			return sent.read(new WriteReader<Integer, MapAccessor<Integer, RequestPacket>>() {
				@Override
				public Integer read(MapAccessor<Integer, RequestPacket> data) {
					int reqID = nextID++;

					while (data.has(reqID))
						reqID = nextID++;
					data.put(reqID, request);
					Logger.debug("Sending Request", reqID, request, client);
					return reqID;
				}
			});
		}
		
		public RequestPacket take(int id) {
			return sent.take(id);
		}
    }

    public static interface Processor {

        public void process(RequestPacket request, Client client, int id);
    }

    protected static final DefaultPropMap<Client, ClientRequests> sendRequests = new DefaultPropMap(new Creator<ClientRequests, Client>() {
        public ClientRequests create(Client client) {
            return new ClientRequests(client);
        }
    }, PropMap.Type.WeakHashMap);

    static void processResponse(Processor processor, final Client client, final int id) {
        RequestPacket request = sendRequests.read(new Reader<RequestPacket, MapAccessor<Client, ClientRequests>>() {
            @Override
            public RequestPacket read(MapAccessor<Client, ClientRequests> data) {
                return data.get(client).take(id);
            }
        });
        if (request == null)
            throw new RuntimeException("No such request was made");

        try {
            processor.process(request, client, id);
        } catch (Throwable t) {
            // TODO: Add aborted work handler
        }
    }

    protected abstract R handleServerRequest(C client);
    protected abstract R handleClientRequest(C client, S server);

	@Override
	protected void aboutToSend(final C client) {
		refID = sendRequests.read(new WriteReader<Integer, MapAccessor<Client, ClientRequests>>() {
            @Override
            public Integer read(MapAccessor<Client, ClientRequests> data) {
				return data.get(client).push(RequestPacket.this);
            }
        });
		super.aboutToSend(client);
	}
	
	protected void failedToComplete(final C client) {}

	@Override
	protected void failedToSend(final C client, Throwable reason) {
		sendRequests.write(new Writer<MapAccessor<Client, ClientRequests>>() {
			@Override
			public void write(MapAccessor<Client, ClientRequests> data) {
				if(data.has(client)) {
					Logger.debug("Request Failed to Send", refID, RequestPacket.this, client);
					ClientRequests requests = data.get(client);
					requests.sent.remove(refID);
				}
			}
        });
		failedToComplete(client);
		super.failedToSend(client, reason);
	}

    @Override
    protected final void recvFromServer(C client) {
        Logger.debug("Handling Request from Server", this, client);
        R response = handleServerRequest(client);
        if (response == null) {
            Logger.gears("No immediate response for request", this, client);
            return;
        }

        Logger.debug("Sending Response to Server", this, client, response);
        client.send(response);
    }

    @Override
    protected final void recvFromClient(C client, S server) {
        Logger.debug("Handling Request from Client", this, client);
        R response = handleClientRequest(client, server);
        if (response == null) {
            Logger.gears("No immediate response for request", this, client);
            return;
        }

        Logger.debug("Sending Response to Server", this, client, response);
        client.send(response);
    }

}
