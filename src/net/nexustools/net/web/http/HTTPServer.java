/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.web.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import net.nexustools.net.Client;
import net.nexustools.net.Packet;
import net.nexustools.net.SimplePacketTransport;
import net.nexustools.net.web.WebHeaders;
import net.nexustools.net.web.WebPacket;
import net.nexustools.net.web.WebRequest;
import net.nexustools.net.web.WebResponse;
import net.nexustools.net.web.WebServer;
import net.nexustools.net.web.modules.WebModule;
import net.nexustools.runtime.RunQueue;

/**
 *
 * @author kate
 */
public class HTTPServer<P extends WebPacket, C extends Client<P, ? extends HTTPServer>> extends WebServer<P, C> {
	
	public static class HTTPTransport extends SimplePacketTransport<WebPacket> {
		@Override
		public WebPacket create(int id) {
			return new HTTPRequest();
		}
	}

    public HTTPServer(WebModule module, int port, Transport protocol, RunQueue runQueue) throws IOException {
        super(module, port, protocol, new HTTPTransport(), runQueue);
    }
    public HTTPServer(WebModule module, int port, Transport protocol) throws IOException {
        super(module, port, protocol, new HTTPTransport());
    }

	@Override
	protected WebResponse createResponseImpl(int code, String codeMessage, WebHeaders headers, InputStream payload, WebRequest request) {
		return new HTTPResponse(code, codeMessage, headers, payload);
	}
    
}
