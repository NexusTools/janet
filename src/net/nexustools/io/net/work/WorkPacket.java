/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.io.net.work;

import net.nexustools.io.net.RequestPacket;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author kate
 */
public abstract class WorkPacket<R extends WorkResponsePacket, C extends WorkClient, S extends WorkServer> extends RequestPacket<R, C, S> {
   
    protected abstract R processWork(C client);

	@Override
	protected void failedToComplete(C client) {
		Logger.quote("Recovering failed work", this);
		((WorkServer)client.server()).pushWork(this);
	}

    @Override
    protected R handleServerRequest(C client) {
		return processWork(client);
    }

    @Override
    protected R handleClientRequest(C client, S server) {
        throw new RuntimeException("The server cannot do work.");
    }
    
}
