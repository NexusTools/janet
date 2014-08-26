/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web;

import net.nexustools.data.buffer.basic.StrongTypeList;
import net.nexustools.janet.Client;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author katelyn
 */
public abstract class WebResponse<T, C extends Client, S extends WebServer> extends WebPacket<T, C, S> {
	
	StrongTypeList<Runnable> finishers = new StrongTypeList();
	public void onFinish(Runnable... finisher) {
		finishers.pushAll(finisher);
	}

	@Override
	protected void failedToComplete(C client, Throwable reason) {
		super.failedToComplete(client, reason);
		finish();
	}
	@Override
	protected void sendComplete(C client) {
		super.sendComplete(client);
		finish();
	}
	protected void finish() {
		for(Runnable finisher : finishers.take())
			try {
				finisher.run();
			} catch(Throwable t) {
				Logger.exception(Logger.Level.Warning, t);
			}
	}
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if(finishers.length() > 0) {
			Logger.warn("Response still has installed finishers on finalize", this);
			finish();
		}
	}

    @Override
    protected void recvFromClient(C client, S server) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	public abstract int status();
	public abstract String statusMessage();
	public abstract WebHeaders headers();
	
}
