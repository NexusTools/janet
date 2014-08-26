/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import net.nexustools.janet.Client;
import net.nexustools.janet.Packet;
import net.nexustools.janet.SimplePacketTransport;
import net.nexustools.web.WebHeaders;
import net.nexustools.web.WebPacket;
import net.nexustools.web.WebRequest;
import net.nexustools.web.WebResponse;
import net.nexustools.web.WebServer;
import net.nexustools.web.handlers.WebRequestHandler;
import net.nexustools.runtime.RunQueue;

/**
 *
 * @author kate
 */
public class HTTPServer<P extends WebPacket, C extends Client<P, ? extends HTTPServer>> extends WebServer<P, C> {
	
	public static class HTTPTransport extends SimplePacketTransport<WebPacket> {
		final RunQueue runQueue;
		public HTTPTransport(RunQueue runQueue) {
			this.runQueue = runQueue;
		}
		@Override
		public WebPacket create(int id) {
			return new HTTPRequest(runQueue);
		}
	}

    public HTTPServer(WebRequestHandler module, int port, Protocol protocol, RunQueue runQueue, Object... args) throws IOException {
        super(module, port, protocol, new HTTPTransport(runQueue), runQueue, args);
    }
    public HTTPServer(WebRequestHandler module, int port, Protocol protocol, Object... args) throws IOException {
        super(module, port, protocol, new HTTPTransport(RunQueue.current()), args);
    }

	@Override
	protected WebResponse createResponseImpl(int code, String codeMessage, WebHeaders headers, InputStream payload, WebRequest request) {
		return new HTTPResponse(code, codeMessage, headers, payload);
	}
    
}
