/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web.handlers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import net.nexustools.io.EfficientInputStream;
import net.nexustools.web.CGIException;
import net.nexustools.web.ConnectionClosedListener;
import net.nexustools.web.WebRequest;
import net.nexustools.web.WebResponse;
import net.nexustools.web.WebServer;
import net.nexustools.web.http.HTTPHeaders;
import net.nexustools.utils.Pair;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author kate
 */
public class CGIRequestHandler implements WebRequestHandler {
	
	private final String cgiScript;
	private final String cgiBinary;
	private final String documentRoot;
	public CGIRequestHandler(String documentRoot, String cgiScript, String cgiBinary) {
		this.documentRoot = documentRoot;
		this.cgiBinary = cgiBinary;
		this.cgiScript = cgiScript;
	}

	public WebResponse handle(WebServer server, WebRequest request) throws Throwable {
		return createResponse(documentRoot, cgiScript, cgiBinary, server, request);
	}
	
	public static WebResponse createResponse(String documentRoot, String cgiScript, String cgiProcess, WebServer server, WebRequest request) throws IOException {
		if(!documentRoot.endsWith("/"))
			documentRoot += '/';
		if(cgiScript.startsWith("/"))
			cgiScript = cgiScript.substring(1);
		
		try {
			File rootFile = new File(cgiProcess);
			if(!rootFile.exists())
				return server.systemPage(404, "CGI Script Missing", cgiProcess + " does not exist.", request);
			if(!rootFile.canRead())
				return server.systemPage(403, "CGI Permission Denied", cgiProcess + " does not have read access from the server.", request);
			if(!rootFile.canExecute())
				return server.systemPage(403, "CGI Permission Denied", cgiProcess + " does not have executable access from the server.", request);
			rootFile = new File(documentRoot);
			if(!rootFile.exists())
				return server.systemPage(404, "Document Root Missing", documentRoot + " does not exist.", request);
			if(!rootFile.canRead())
				return server.systemPage(403, "Root Permission Denied", documentRoot + " does not have read access from the server.", request);
			ProcessBuilder builder = new ProcessBuilder(cgiProcess).directory(rootFile);
			
			String scriptPath = documentRoot + cgiScript;
			rootFile = new File(scriptPath);
			if(!rootFile.exists())
				return server.systemPage(404, "File Not Found", scriptPath + " does not exist.", request);
			if(!rootFile.canRead())
				return server.systemPage(403, "Root Permission Denied", scriptPath + " does not have read access from the server.", request);

			Map<String, String> env = builder.environment();
			env.clear();
			
			env.put("DOCUMENT_ROOT", documentRoot);
			for(Pair<String, List<String>> header : request.headers()) {
				if(header.v.size() == 1)
					env.put("HTTP_" + (header.i.toUpperCase().replace('-', '_')), header.v.get(0));
			}
			//env.add("HTTPS=on");
			env.put("PATH", System.getenv("PATH"));// Pass the path
			env.put("REDIRECT_STATUS", "1");
			env.put("GATEWAY_INTERFACE", "CGI/1.1");
			env.put("QUERY_STRING", request.requestString(WebRequest.Scope.GET));
			//env.add("REMOTE_ADDR=");
			//env.add("REMOTE_HOST=");
			//env.add("REMOTE_PORT=");
			//env.add("REMOTE_USER=");
			env.put("REQUEST_METHOD", request.method());
			env.put("REQUEST_URI", request.path());
			env.put("SCRIPT_FILENAME", scriptPath);
			env.put("SCRIPT_NAME", "/" + cgiScript);
			env.put("SERVER_NAME", server.serverName());
			env.put("SERVER_PORT", "8080");
			env.put("SERVER_SOFTWARE", "JaNET");

			Logger.debug("Launching CGI", env);
			final Process proc = builder.start();
			final InputStream cgiStream = new BufferedInputStream(proc.getInputStream());
			request.addConnectionListener(new ConnectionClosedListener() {
				@Override
				public void connectionClosed(ConnectionClosedListener.ConnectionClosedEvent event) {
					try {
					proc.exitValue();
					} catch(IllegalThreadStateException ex) {
						Logger.warn("Client disconnected before cgi script finished, ending process.");
						proc.destroy();
						try {
							cgiStream.close();
						} catch (IOException e) {}
					}
				}
			});
			Logger.debug("Waiting on CGI Completion");

			HTTPHeaders headers = new HTTPHeaders();
			try {
				headers.parse(cgiStream);
			} catch(Throwable t) {
				Logger.debug(headers);
				throw new CGIException("Cannot Parse Headers", t);
			}

			int status = 200;
			String statusMessage = "OK";
			String statusHeader = headers.take("Status");
			if(statusHeader != null) {
				int sep = statusHeader.indexOf(' ');
				try {
					status = Integer.valueOf(statusHeader.substring(0, sep));
					statusMessage = statusHeader.substring(sep+1);
				} catch(Throwable t) {
					Logger.exception(Logger.Level.Gears, t);
				}
			}

			Logger.debug(headers);
			return server.createRawResponse(status, statusMessage, headers, new EfficientInputStream() {
				boolean allowDeath = false;
				@Override
				public int read(byte[] b, int off, int len) throws IOException {
					int read;
					while((read = cgiStream.read(b, off, len)) < 1) {
						try {
							if(!allowDeath) {
								allowDeath = true;
								Logger.debug("CGI Exited with Status", proc.exitValue());
								throw new IllegalThreadStateException();
							} else
								break;
						} catch(IllegalThreadStateException stillRunning) {
							try {
								Thread.sleep(20);
							} catch (InterruptedException ex) {}
						}
					}
					Logger.debug("Read CGI Data", read);
					return read;
				}
			}, request);
		} catch(CGIException cgi) {
			throw cgi;
		} catch(Throwable t) {
			throw new CGIException("Unable to Start CGI Process", t);
		}
	}
    
}
