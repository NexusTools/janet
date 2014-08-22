/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.io.net.work;

import java.io.IOException;
import java.net.Socket;
import net.nexustools.concurrent.PropList;
import net.nexustools.io.net.Packet;
import net.nexustools.io.net.PacketRegistry;
import net.nexustools.io.net.Server;
import net.nexustools.runtime.RunQueue;

/**
 *
 * @author kate
 */
public abstract class WorkServer<W extends WorkPacket, P extends Packet, C extends WorkClient<W, P, ? extends WorkServer>> extends Server<P, C> {

	final PropList<C> clientQueue = new PropList();
    private final PropList<W> workQueue = new PropList();
    public WorkServer(int port, Protocol protocol, PacketRegistry packetRegistry) throws IOException {
        super(port, protocol, packetRegistry);
    }
    public WorkServer(int port, Protocol protocol, PacketRegistry packetRegistry, RunQueue runQueue) throws IOException {
        super(port, protocol, packetRegistry, runQueue);
    }

    @Override
    public C createClient(Socket socket) throws IOException {
        return (C) new WorkClient(socket, this);
    }
    
    public W nextWork(C client) {
        return workQueue.shift();
    }
    
    public void pushWork(W work) {
		C nextClient = clientQueue.shift();
		if(nextClient == null)
			workQueue.push(work);
		else
			nextClient.send((P)work);
    }
    
}
