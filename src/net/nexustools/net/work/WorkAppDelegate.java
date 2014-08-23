/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.work;

import java.io.IOException;
import java.net.Socket;
import net.nexustools.net.DefaultPacketTransport;
import net.nexustools.net.PacketTransport;
import net.nexustools.net.Server.Transport;
import net.nexustools.net.ServerAppDelegate;

/**
 *
 * @author kate
 */
public abstract class WorkAppDelegate<C extends WorkClient, S extends WorkServer> extends ServerAppDelegate<C, S> {

    public WorkAppDelegate(String[] args, String name, String organization, PacketTransport packetRegistry, float multiplier) {
        super(args, name, organization, packetRegistry, multiplier);
    }
    public WorkAppDelegate(String[] args, String name, String organization, float multiplier) {
        this(args, name, organization, new DefaultPacketTransport(), multiplier);
    }
    public WorkAppDelegate(String[] args, String name, String organization) {
        this(args, name, organization, 2.0f);
    }
    
    public abstract WorkPacket nextWork(C client);

    @Override
    protected void populate(PacketTransport registry) throws NoSuchMethodException{
        registry.register(RequestWorkPacket.class);
    }
    
    @Override
    protected C createClient(String host, int port) throws IOException {
        return (C)new WorkClient(host, port, Transport.TCP, runQueue, packetRegistry);
    }
    
    @Override
    protected C createClient(Socket socket, S server) throws IOException {
        return (C)new WorkClient(socket, server);
    }

    @Override
    protected S createServer(int port) throws IOException {
        return (S)new WorkServer(port, Transport.TCP, packetRegistry, runQueue) {
            @Override
            public WorkPacket nextWork(WorkClient client) {
                WorkPacket work = super.nextWork(client);
                if(work == null)
                    work = WorkAppDelegate.this.nextWork((C)client);
                return work;
            }
            @Override
            public WorkClient createClient(Socket socket) {
                try {
                    return WorkAppDelegate.this.createClient(socket, (S)this); //To change body of generated methods, choose Tools | Templates.
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    @Override
    protected void launchClient(C client) {}
    
}
