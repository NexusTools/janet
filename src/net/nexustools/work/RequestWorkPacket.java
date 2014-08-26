/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.work;

import net.nexustools.janet.SimplePacket;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author kate
 */
public final class RequestWorkPacket<T, C extends WorkClient, S extends WorkServer> extends SimplePacket<T, C, S> {

    @Override
    protected void recvFromServer(C client) {
        throw new RuntimeException("The server should not be requesting work from a client");
    }

    @Override
    protected void recvFromClient(C client, S server) {
        WorkPacket workPacket = server.nextWork(client);
        if(workPacket == null) {
            Logger.info("No more new work, will send more when available.");
            // TODO: Queue client for future work
            return;
        }
        
        client.send(workPacket);
    }
    
}
