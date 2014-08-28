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
import java.io.OutputStream;
import java.util.Map;
import net.nexustools.data.buffer.basic.StringList;
import net.nexustools.io.FileStream;
import net.nexustools.io.Stream;
import net.nexustools.io.StreamUtils;
import net.nexustools.utils.Pair;
import net.nexustools.utils.StringUtils;
import net.nexustools.utils.log.Logger;
import net.nexustools.web.CGIException;
import net.nexustools.web.WebRequest;
import net.nexustools.web.WebResponse;
import net.nexustools.web.WebServer;
import net.nexustools.web.http.HTTPHeaders;
import net.nexustools.web.io.ChunkedEncodingInputStream;

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
		Stream stream = Stream.open(documentRoot).effectiveStream();
		if(!(stream instanceof FileStream))
			throw new UnsupportedOperationException(stream + " is not an applicable document root.");
		
		documentRoot = stream.path();
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
			for(Pair<String, StringList> header : request.headers()) {
				if(header.v.length() == 1)
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
			
			long payloadLength = request.payloadLength();
			if(payloadLength > 0) {
				env.put("CONTENT_LENGTH", String.valueOf(payloadLength));
				env.put("CONTENT_TYPE", String.valueOf(request.payloadType()));
			}

			Logger.debug("Launching CGI", env);
			final Process proc = builder.start();
			final InputStream cgiStream = new BufferedInputStream(proc.getInputStream());
			request.onFinish(new Runnable() {
				public void run() {
					try {
						proc.exitValue();
					} catch(IllegalThreadStateException ex) {
						Logger.warn("Packet ended before cgi script finished, ending process.");
						proc.destroy();
					}
				}
			});
			OutputStream procOut = proc.getOutputStream();
			if(payloadLength > 0) {
				InputStream payloadIn = request.payload().createInputStream();
				Logger.debug("Passing Payload to CGI", StringUtils.stringForSize(payloadLength));
				StreamUtils.copy(payloadIn, procOut, payloadLength);
				payloadIn.close();
			}
			procOut.close();
			
			Logger.debug("Reading CGI Headers");
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

			Logger.debug("Passing CGI STDOUT");
			return server.createRawResponse(status, statusMessage, headers, cgiStream, request);
		} catch(CGIException cgi) {
			throw cgi;
		} catch(Throwable t) {
			throw new CGIException("Unable to Start CGI Process", t);
		}
	}
    
}
