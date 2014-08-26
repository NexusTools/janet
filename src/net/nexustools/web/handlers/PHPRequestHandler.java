/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web.handlers;

import java.io.File;
import java.io.IOException;
import net.nexustools.io.FileStream;
import net.nexustools.utils.log.Logger;
import net.nexustools.web.WebRequest;
import net.nexustools.web.WebResponse;
import net.nexustools.web.WebServer;

/**
 * Looks for various PHP install locations, and uses the correct handler to respond.
 * 
 * 
 * @author katelyn
 */
public class PHPRequestHandler implements WebRequestHandler {
	
	public static interface PHPImpl {
		public WebResponse createResponse(String documentRoot, String phpScript, WebServer server, WebRequest request) throws IOException;
	}
	
	public static enum Type {
		CGI,
		FastCGI,
		HipHopVM,
		Unknown,
		
		None
	}
	
	public static final Type type;
	public static final PHPImpl impl;
	
	static {
		Logger.debug("Detecting PHP Install");
		Type detected = Type.None;
		PHPImpl detectedImpl = null;
		
		if(FastCGIRequestHandler.testConnection("/var/run/php5-fpm.sock")) { // TODO: Figure out how this path can be detected
			detectedImpl = new PHPImpl() {
				public WebResponse createResponse(String documentRoot, String phpScript, WebServer server, WebRequest request) throws IOException {
					return FastCGIRequestHandler.createResponse(documentRoot, phpScript, "/var/run/php5-fpm.sock", server, request);
				}
			};
			detected = Type.CGI;
			Logger.info("Detected php5-fpm.sock");
		} else if(new File("/usr/bin/php5-cgi").canExecute()) { // TODO: Make this use $PATH
			detectedImpl = new PHPImpl() {
				public WebResponse createResponse(String documentRoot, String phpScript, WebServer server, WebRequest request) throws IOException {
					return CGIRequestHandler.createResponse(documentRoot, phpScript, "/usr/bin/php5-cgi", server, request);
				}
			};
			detected = Type.CGI;
			Logger.info("Detected php5-cgi");
		}
		
		if(detectedImpl == null)
			detectedImpl = new PHPImpl() {
				public WebResponse createResponse(String documentRoot, String phpScript, WebServer server, WebRequest request) throws IOException {
					return server.systemPage(501, "PHP Missing", "PHP Install Not Found", "No PHP install could be found on this server.", request);
				}
			};
		impl = detectedImpl;
		type = detected;
	}
	
	final String documentRoot, phpFile;
	public PHPRequestHandler(String documentRoot, String phpFile) {
		this.documentRoot = documentRoot;
		this.phpFile = phpFile;
	}

	public WebResponse handle(WebServer server, WebRequest request) throws Throwable {
		return createResponse(documentRoot, phpFile, server, request);
	}
	
	
	public static WebResponse createResponse(String documentRoot, String phpScript, WebServer server, WebRequest request) throws IOException {
		return impl.createResponse(documentRoot, phpScript, server, request);
	}
	
}
