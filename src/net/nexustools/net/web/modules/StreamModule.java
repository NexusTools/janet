/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.web.modules;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import net.nexustools.net.web.WebRequest;
import net.nexustools.net.web.WebResponse;
import net.nexustools.net.web.WebServer;

/**
 *
 * @author kate
 */
public class StreamModule implements WebModule {
    
    private final String documentRoot;
    public StreamModule(String root) {
        documentRoot = root;
    }
    public StreamModule() {
        this("run://");
    }

    boolean loadedJava7APIs = false;
    Method probeContentType;
    Method pathForUri;
	
    @Override
    public WebResponse handle(WebServer server, WebRequest request) throws IOException, URISyntaxException {
		if(!request.method().equalsIgnoreCase("get") && !request.method().equalsIgnoreCase("post"))
			throw new IOException(request.method() + " method is not supported by StreamModule");
		
		String connection = request.headers().get("connection");
		if(connection != null) {
			if(!connection.equalsIgnoreCase("keep-alive") && connection.equalsIgnoreCase("close")) {
				request.headers().set("Connection", "Close");
				return server.standardResponse(501, "Unknown Connection Type", request);
			}
		}
		
		return server.streamResponse(documentRoot + request.path(), request);
    }
    
}
