/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.work;

import java.io.IOException;
import net.nexustools.data.AdaptorException;
import net.nexustools.io.DataInputStream;

/**
 *
 * @author kate
 */
public abstract class ResponsePacket<W extends WorkPacket, C extends WorkClient<W, ?, ?>, S extends WorkServer<W, ?, ?>> extends BaseWorkPacket<C, S> {

    protected W workRequest;
    protected abstract void handleResponse(C client, S server, W work);

    @Override
    public void read(DataInputStream dataInput, C client) throws UnsupportedOperationException, IOException {
        super.read(dataInput, client);
        workRequest = client.takeByID(workId);
    }
    
    @Override
    protected final void recvFromServer(C client) {
        throw new RuntimeException("Client should not be receiving ResponsePackets");
    }

    @Override
    protected final void recvFromClient(C client, S server) {
        handleResponse(client, server, workRequest);
    }
    
}
