/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web;

import net.nexustools.janet.Client;
import net.nexustools.janet.Packet;

/**
 *
 * @author kate
 */
public abstract class WebPacket<T, C extends Client, S extends WebServer> extends Packet<T, C, S> {

    @Override
    protected void recvFromServer(C client) {
        throw new RuntimeException("Not implemented yet.");
    }
    
}
