/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.work;

/**
 *
 * @author kate
 */
public abstract class ResponsePacket<W extends WorkPacket, C extends WorkClient<W, ?, ?>, S extends WorkServer<W, ?, ?>> extends BaseWorkPacket<C, S> {

    protected abstract void handleResponse(C client, S server, W work);
    
    @Override
    protected final void recvFromServer(C client) {
        throw new RuntimeException("Client should not be receiving ResponsePackets");
    }

    @Override
    protected final void recvFromClient(C client, S server) {
        handleResponse(client, server, client.takeByID(workId));
    }
    
}