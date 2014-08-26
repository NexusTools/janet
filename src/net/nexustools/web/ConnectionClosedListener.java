/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web;

import java.util.EventListener;
import net.nexustools.event.Event;

/**
 *
 * @author katelyn
 */
public abstract class ConnectionClosedListener<S> implements EventListener {
	
	public static class ConnectionClosedEvent<S> extends Event<S> {
		public ConnectionClosedEvent(S source) {
			super(source);
		}
	}
	
	public abstract void connectionClosed(ConnectionClosedEvent<S> event);
    
}
