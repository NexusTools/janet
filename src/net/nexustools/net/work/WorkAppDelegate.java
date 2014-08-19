/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.work;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.nexustools.io.DataInputStream;
import net.nexustools.io.DataOutputStream;
import net.nexustools.io.net.Server.Protocol;
import net.nexustools.io.net.ServerAppDelegate;
import net.nexustools.utils.Pair;

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
        return (C)new WorkClient(name + "-WorkClient", host, port, Protocol.TCP, packetRegistry);
    }
    
    @Override
    protected C createClient(Pair<DataInputStream,DataOutputStream> socket, S server) throws IOException {
        return (C)new WorkClient(name + "-WorkClient", socket, packetRegistry);
    }

    @Override
    protected S createServer(int port) throws IOException {
        return (S)new WorkServer(port, Protocol.TCP, packetRegistry) {
            @Override
            public WorkPacket nextWork(WorkClient client) {
                return WorkAppDelegate.this.nextWork((C)client);
            }
            @Override
            public WorkClient createClient(Pair socket) {
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
