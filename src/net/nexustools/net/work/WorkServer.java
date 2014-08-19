/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.work;

import java.io.IOException;
import net.nexustools.concurrent.PropList;
import net.nexustools.concurrent.PropMap;
import net.nexustools.io.net.Client;
import net.nexustools.io.net.Packet;
import net.nexustools.io.net.PacketRegistry;
import net.nexustools.io.net.Server;
import net.nexustools.runtime.RunQueue;
import net.nexustools.utils.Pair;

/**
 *
 * @author kate
 */
public abstract class WorkServer<W extends WorkPacket, P extends Packet, C extends WorkClient<W, P, ? extends WorkServer>> extends Server<P, C> {

    private final PropList<W> workQueue = new PropList();
    private final PropMap<Client, W> workMap = new PropMap();
    public WorkServer(int port, Protocol protocol, PacketRegistry packetRegistry) throws IOException {
        super(port, protocol, packetRegistry);
    }
    public WorkServer(int port, Protocol protocol, PacketRegistry packetRegistry, RunQueue runQueue) throws IOException {
        super(port, protocol, packetRegistry, runQueue);
    }

    @Override
    public C createClient(Pair socket) {
        return (C) new WorkClient("WorkClient", socket, this);
    }
    
    public W nextWork(C client) {
        return workQueue.shift();
    }
    
    public void pushWork(W work) {
        workQueue.push(work);
        // TODO: Notify waiting clients
    }
    
}
