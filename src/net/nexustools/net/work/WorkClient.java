/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.work;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.nexustools.concurrent.PropMap;
import net.nexustools.io.DataInputStream;
import net.nexustools.io.DataOutputStream;
import net.nexustools.io.net.Client;
import net.nexustools.io.net.ClientListener;
import net.nexustools.io.net.Packet;
import net.nexustools.io.net.PacketRegistry;
import net.nexustools.io.net.Server.Protocol;
import net.nexustools.utils.Pair;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author kate
 */
public class WorkClient<W extends WorkPacket, P extends Packet, S extends WorkServer<W, P, ? extends WorkClient>> extends Client<P, S> {

    private final PropMap<Long, W> sentWork = new PropMap();
    private final AtomicInteger atomicInteger = new AtomicInteger();
    public WorkClient(String name, Pair<DataInputStream, DataOutputStream> socket, final WorkServer server) {
        super(name, socket, server);
        addClientListener(new ClientListener() {
            @Override
            public void clientConnected(ClientListener.ClientEvent clientConnectedEvent) {}
            @Override
            public void clientDisconnected(ClientListener.ClientEvent clientConnectedEvent) {
                Map<Long, W> remaining = sentWork.take();
                
                if(remaining.size() > 0) {
                    Logger.info("Recovering" + remaining.size() + "Work Entries");
                    for(Map.Entry<Long, W> work : remaining.entrySet())
                        server.pushWork(work.getValue());
                }
            }
        });
    }
    public WorkClient(String name, final Pair<DataInputStream,DataOutputStream> socket, PacketRegistry packetRegistry) {
        super(name, socket, packetRegistry);
        init();
    }
    public WorkClient(String name, String host, int port, Protocol protocol, PacketRegistry packetRegistry) throws IOException {
        super(name, host, port, protocol, packetRegistry);
        init();
    }
    
    private void init() {
        Logger.quote("Requesting Work", this);
        send((P)new RequestWorkPacket());
    }
    
    public void send(W work) {
        work.workId = atomicInteger.getAndIncrement();
        sentWork.put(work.workId, work);
        super.send((P)work); // incase W and P overlap
    }
    
    W takeByID(long workID) {
        return sentWork.take(workID);
    }
    
}
