/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.work;

import java.io.IOException;
import net.nexustools.io.DataInputStream;
import net.nexustools.io.DataOutputStream;
import net.nexustools.io.net.Packet;
import net.nexustools.io.net.SimplePacket;

/**
 *
 * @author kate
 */
public final class RequestWorkPacket<C extends WorkClient, S extends WorkServer> extends SimplePacket<C, S> {

    @Override
    protected void recvFromServer(C client) {
        throw new RuntimeException("The server should not be requesting work from a client");
    }

    @Override
    protected void recvFromClient(C client, S server) {
        client.send(server.nextWork(client));
    }
    
}
