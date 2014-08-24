/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.web;

import net.nexustools.net.Client;

/**
 *
 * @author katelyn
 */
public abstract class WebResponse<T, C extends Client, S extends WebServer> extends WebPacket<T, C, S> {

    @Override
    protected void recvFromClient(C client, S server) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	public abstract int status();
	public abstract String statusMessage();
	public abstract WebHeaders headers();
	
}
