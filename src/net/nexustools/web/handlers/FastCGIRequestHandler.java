/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web.handlers;

import java.io.File;
import java.io.IOException;
import net.nexustools.web.CGIException;
import net.nexustools.web.WebRequest;
import net.nexustools.web.WebResponse;
import net.nexustools.web.WebServer;

/**
 *
 * @author kate
 */
public class FastCGIRequestHandler implements WebRequestHandler {

	static boolean testConnection(String string) {
		return new File(string).canWrite();
	}
	
	private final String cgiScript;
	private final String cgiBinary;
	private final String documentRoot;
	public FastCGIRequestHandler(String documentRoot, String cgiScript, String cgiBinary) {
		this.documentRoot = documentRoot;
		this.cgiBinary = cgiBinary;
		this.cgiScript = cgiScript;
	}

	public WebResponse handle(WebServer server, WebRequest request) throws Throwable {
		return createResponse(documentRoot, cgiScript, cgiBinary, server, request);
	}
	
	public static WebResponse createResponse(String documentRoot, String cgiScript, String fastCGISocket, WebServer server, WebRequest request) throws IOException {
		if(!documentRoot.endsWith("/"))
			documentRoot += '/';
		if(cgiScript.startsWith("/"))
			cgiScript = cgiScript.substring(1);
		
		try {
			File rootFile = new File(documentRoot);
			if(!rootFile.exists())
				return server.systemPage(404, "Document Root Missing", documentRoot + " does not exist.", request);
			if(!rootFile.canRead())
				return server.systemPage(403, "Root Permission Denied", documentRoot + " does not have read access from the server.", request);
			
			throw new CGIException("Not implemented yet.");
		} catch(CGIException cgi) {
			throw cgi;
		} catch(Throwable t) {
			throw new CGIException("Unable to connect to FastCGI gateway.", t);
		}
	}
    
}
