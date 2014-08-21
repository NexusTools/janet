/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.io.net.work;

import java.io.IOException;
import java.net.Socket;
import net.nexustools.io.net.Client;
import net.nexustools.io.net.Packet;
import net.nexustools.io.net.PacketRegistry;
import net.nexustools.io.net.Server.Protocol;
import net.nexustools.runtime.RunQueue;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author kate
 */
public class WorkClient<W extends WorkPacket, P extends Packet, S extends WorkServer<W, P, ? extends WorkClient>> extends Client<P, S> {

    public WorkClient(String name, Socket socket, final WorkServer server) throws IOException {
        super(name, socket, server);
    }
    public WorkClient(String name, String host, int port, Protocol protocol, RunQueue runQueue, PacketRegistry packetRegistry) throws IOException {
        super(name, host, port, protocol, runQueue, packetRegistry);
        requestWork();
    }
    
    private void requestWork() {
        Logger.quote("Requesting Work", this);
        super.send((P)new RequestWorkPacket());
    }

	@Override
	public void send(P packet) {
		super.send(packet);
		if(packet instanceof WorkResponsePacket)
			requestWork();
	}
    
}
