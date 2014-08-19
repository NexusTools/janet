/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.work;

import net.nexustools.utils.log.Logger;

/**
 *
 * @author kate
 */
public abstract class WorkPacket<R extends ResponsePacket, C extends WorkClient, S extends WorkServer> extends BaseWorkPacket<C, S> {
   
    protected abstract R processWork(C client);
    
    @Override
    protected final void recvFromServer(C client) {
        Logger.quote("processing Work", this);
        R response = processWork(client);
        response.workId = workId;
        Logger.quote("Finished Work", this);
        client.send(response);
        
        Logger.quote("Requesting Work", this);
        client.send(new RequestWorkPacket());
    }

    @Override
    protected final void recvFromClient(C client, S server) {
        throw new RuntimeException("The server should not be receiving WorkPacket's...");
    }
    
}
