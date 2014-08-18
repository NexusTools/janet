/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.work;

import java.io.IOException;
import net.nexustools.io.net.PacketRegistry;
import net.nexustools.io.net.Server.Protocol;
import net.nexustools.io.net.ServerAppDelegate;

/**
 *
 * @author kate
 */
public abstract class WorkAppDelegate<C extends WorkClient, S extends WorkServer> extends ServerAppDelegate<C, S> {

    public WorkAppDelegate(String[] args, String name, String organization) {
        super(args, name, organization);
    }
    
    public abstract WorkPacket nextWork(C client);

    @Override
    protected C createClient(String host, int port) throws IOException {
        return (C)new WorkClient("WorkClient", host, port, Protocol.TCP, packetRegistry);
    }

    @Override
    protected S createServer(int port) throws IOException {
        return (S)new WorkServer(port, Protocol.TCP, packetRegistry) {
            @Override
            public WorkPacket nextWork(WorkClient client) {
                return WorkAppDelegate.this.nextWork((C)client);
            }
        };
    }

    @Override
    protected void launchClient(C client) {}
    
}
