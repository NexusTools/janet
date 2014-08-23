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
		return server.streamResponse(documentRoot + request.path(), request.path());
    }
    
}
