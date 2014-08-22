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
import net.nexustools.io.DataInputStream;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author kate
 */
public abstract class ResponsePacket<T, R extends RequestPacket, C extends Client, S extends Server> extends RefPacket<T, C, S> {
	
    protected abstract void handleServerResponse(C client, R request);
    protected abstract void handleClientResponse(C client, S server, R request);

	@Override
	public void read(DataInputStream dataInput, C client) throws UnsupportedOperationException, IOException {
		super.read(dataInput, client);
		RequestPacket.checkResponse(client, refID);
	}

	@Override
	protected final void recvFromServer(C client) {
		Logger.debug("Handling Response from Server");
		RequestPacket.processResponse(new RequestPacket.Processor() {
			public void process(RequestPacket request, Client client, int id) {
				handleServerResponse((C)client, (R)request);
			}
		}, client, refID);
	}

	@Override
	protected final void recvFromClient(C client, final S server) {
		Logger.debug("Handling Response from Client");
		RequestPacket.processResponse(new RequestPacket.Processor() {
			public void process(RequestPacket request, Client client, int id) {
				handleClientResponse((C)client, server, (R)request);
			}
		}, client, refID);
	}
	
}
