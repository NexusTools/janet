/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.web;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import net.nexustools.Application;
import net.nexustools.concurrent.Prop;
import net.nexustools.io.EfficientInputStream;
import net.nexustools.io.Stream;
import net.nexustools.net.Client;
import net.nexustools.net.PacketTransport;
import net.nexustools.net.Server;
import net.nexustools.net.web.http.HTTPHeaders;
import net.nexustools.net.web.modules.WebModule;
import net.nexustools.runtime.RunQueue;
import net.nexustools.utils.Pair;
import net.nexustools.utils.StringUtils;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author kate
 */
public abstract class WebServer<P extends WebPacket, C extends Client<P, ? extends WebServer>> extends Server<P, C> {
	
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    private Prop<WebModule> module;
	protected final ArrayList<String> systemStyles = new ArrayList();
	protected final ArrayList<String> systemScripts = new ArrayList();
    public WebServer(WebModule module, int port, Transport protocol, PacketTransport registry, RunQueue runQueue) throws IOException {
        super(port, protocol, registry, runQueue);
        this.module = new Prop(module);
		init();
    }
    public WebServer(WebModule module, int port, Transport protocol, RunQueue runQueue) throws IOException {
        super(port, protocol, runQueue);
        this.module = new Prop(module);
		init();
    }
    public WebServer(WebModule module, int port, Transport protocol, PacketTransport registry) throws IOException {
        super(port, protocol, registry);
        this.module = new Prop(module);
		init();
    }
    public WebServer(WebModule module, int port, Transport protocol) throws IOException {
        super(port, protocol);
        this.module = new Prop(module);
		init();
    }
    public WebServer(WebModule module, int port) throws IOException {
        this(module, port, Transport.TCP);
    }
    public WebServer(WebModule module) throws IOException {
        this(module, 80);
    }
	
	private void init() {
		systemStyles.add("resource:/net/nexustools/net/web/System.css");
	}
	
	protected String serverName() {
		return Application.name();
	}
	
	protected String summaryForCode(int code) {
		switch(code) {
			case 200:
				return "OK";
				
			case 304:
				return "No Modifications";
				
			case 403:
				return "Insufficient Permissions";
				
			case 404:
				return "No Handler for Path";
				
			case 500:
				return "Unhandled Exception Thrown";
				
			case 501:
				return "Not Supported Yet";
		}
		
		return "Error " + code;
	}
	
	protected String bodyForCode(int code) {
		switch(code) {
			case 403:
				return "The content you're attempting to reach is protected, authorization is required to view it.";
				
			case 404:
				return "No Handler for Path";
				
			case 500:
				return "Unhandled Exception Thrown";
				
			case 501:
				return "Not Supported Yet";
		}
		
		return "???";
	}
	
	protected void listingHTMLContent(StringBuilder builder, Stream stream, Map<String, String> vars, String path) throws IOException {
		builder.append("<table><tr><th></th><th>Filename</th><th>Type</th><th>Last Modified</th><th>Size</th></tr>");
		for(String child : stream.children()) {
			Stream childStream = null;
			String childPath = path + child;
			builder.append("<tr");
			try {
				childStream = Stream.open(childPath);
				if(childStream.isHidden())
					builder.append(" class=\"hidden\"");
			} catch(Exception ex) {
				childStream = null;
			}
			builder.append("><td style=\"width: 48px\">");
			String type = "unknown";
			boolean preview = false;
			String mimeType = null;
			try {
				if(childStream.hasChildren()) {
					mimeType = "inode/directory";
					type = "folder";
				} else {
					mimeType = childStream.mimeType();
					if(mimeType.startsWith("image/")) {
						preview = true;
						type = "image";
					} else if(mimeType.startsWith("video/")) {
						preview = true;
						type = "video";
					} else if(mimeType.startsWith("audio/")) {
						preview = true;
						type = "audio";
					} else if(mimeType.equals("application/pdf")) {
						type = "pdf";
					} else if(mimeType.equals("application/xml") ||
								mimeType.equals("text/xml")) {
						type = "xml";
					} else if(mimeType.startsWith("text/"))
						type = "text";
				}
			} catch(Throwable t) {
				mimeType = "unknown";
			}
			if(preview) {
				builder.append("<object width=\"24\" height=\"24\" data=\"");
				builder.append(childPath);
				builder.append("?scale=24x24\" type=\"");
				builder.append(mimeType);
				builder.append("\">");
			}
			builder.append("<img width=\"24\" height=\"24\" src=\"");
			builder.append("//raw.githubusercontent.com/NexusTools/NexusFramework/master/resources/icons/22/");
			builder.append(type);
			builder.append(".png\" />");
			if(preview)
				builder.append("</object>");
			builder.append("</td><td style=\"width: 100%\"><a href='");
			builder.append(childPath);
			builder.append("'>");
			builder.append(child);
			builder.append("</a></td><td>");
			try {
				builder.append(mimeType);
			} catch(Throwable t) {}
			builder.append("</td><td>");
			try {
				builder.append(WebServer.dateFormat.format(childStream.lastModified()));
			} catch (Throwable t) {
				builder.append("<error");
				String message = t.getMessage();
				if(message != null) {
					builder.append(" title=\"");
					builder.append(message);
					builder.append("\"");
				}
				builder.append(">");
				builder.append(t.getClass().getSimpleName());
				builder.append("</error>");
			}
			builder.append("</td><td>");
			try {
				builder.append(childStream.sizeStr());
			} catch (Throwable t) {
				builder.append("<error");
				String message = t.getMessage();
				if(message != null) {
					builder.append(" title=\"");
					builder.append(message);
					builder.append("\"");
				}
				builder.append(">");
				builder.append(t.getClass().getSimpleName());
				builder.append("</error>");
			}
			builder.append("</td></tr>");
		}
		builder.append("</table>");
	}

	public final WebResponse cgiResponse(String documentRoot, String cgiScript, String cgiProcess, WebRequest request) throws IOException {
		if(!documentRoot.endsWith("/"))
			documentRoot += '/';
		
		try {
			ProcessBuilder builder = new ProcessBuilder(cgiProcess).redirectError(ProcessBuilder.Redirect.INHERIT).directory(new File(documentRoot));

			Map<String, String> env = builder.environment();
			env.clear();
			
			env.put("DOCUMENT_ROOT", documentRoot + '/');
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
			env.put("SCRIPT_FILENAME", documentRoot + cgiScript);
			env.put("SCRIPT_NAME", cgiScript);
			env.put("SERVER_NAME", serverName());
			env.put("SERVER_PORT", "8080");
			env.put("SERVER_SOFTWARE", "JaNET");

			Logger.debug("Launching CGI", env);
			final Process proc = builder.start();
			final InputStream cgiStream = new BufferedInputStream(proc.getInputStream());
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


			Logger.debug(headers.headers);
			return createResponse(status, statusMessage, headers, new EfficientInputStream() {
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
	
	public final WebResponse streamResponse(Stream stream, WebRequest request) throws IOException {
		if(!stream.canRead())
			return standardResponse(403, request);
		
		if(stream.exists())
			if(stream.hasChildren()) {
				String path = request.path();
				if(!path.endsWith("/"))
					path += "/";
				
				StringBuilder builder = new StringBuilder();
				String format = request.headers().get("format", "html");
				
				if(format.equalsIgnoreCase("html"))
					listingHTMLContent(builder, stream, request.request(WebRequest.Scope.GET_POST), path);
				else
					throw new UnsupportedOperationException("The requested format is not supported: " + format);
				
				return systemPage(200, "Index Of: " + path, builder.toString(), request);
			} else
				return createResponse(200, stream.mimeType(), stream.size(), stream.createInputStream(), request);
		else
			return standardResponse(404, "No such file or directory.", request);
	}
	public final WebResponse streamResponse(String url, WebRequest request) throws IOException, URISyntaxException {
		return streamResponse(Stream.open(url), request);
	}
	
	public final WebResponse exceptionResponse(Throwable t, WebRequest request) {
		return systemPage(500, t.getClass().getSimpleName(), "<pre>" + StringUtils.stringForException(t) + "</pre>", request);
	}
	public final WebResponse standardResponse(int code, WebRequest request) {
		return standardResponse(code, summaryForCode(code), request);
	}
	public final WebResponse standardResponse(int code, String codeMessage, WebRequest request) {
		return systemPage(code, codeMessage, codeMessage, bodyForCode(code), request);
	}
	public final WebResponse systemPage(int code, String title, String body, WebRequest request) {
		return systemPage(code, summaryForCode(code), title, body, request);
	}
	public WebResponse systemPage(int code, String codeMessage, String title, String body, WebRequest request) {
		String connection = request.headers().get("connection");
		if(connection != null) {
			if(!connection.equalsIgnoreCase("keep-alive") && connection.equalsIgnoreCase("close")) {
				request.headers().set("Connection", "Close");
				return standardResponse(501, "Unknown Connection Type", request);
			}
		}
		
		StringBuilder html = new StringBuilder();
		html.append("<html><head><title>");
		html.append(title);
		html.append("</title>");
		
		for(String style : systemStyles) {
			try {
				String source = StringUtils.read(style, StringUtils.UTF8);
				html.append("<style>\n");
				html.append(source);
				html.append("</style>");
			} catch (Throwable t) {
				Logger.exception(Logger.Level.Debug, t);
			}
		}
		
		html.append("</head><body><h1>");
		html.append(title);
		html.append("</h1>");
		html.append(body);
		
		for(String script : systemScripts) {
			try {
				String source = StringUtils.read(script, StringUtils.UTF8);
				html.append("<script>\n");
				html.append(source);
				html.append("</script>");
			} catch (Throwable t) {
				Logger.exception(Logger.Level.Gears, t);
			}
		}
		
		html.append("</body></html>");
		return createResponse(code, codeMessage, html.toString(), false, request);
	}
	
	public final WebResponse createResponse(int code, String codeMessage, String payload, boolean text, WebRequest request, Pair<String,String>... extraHeaders) {
		return createResponse(code, codeMessage, text ? "text/plain" : "text/html", payload.getBytes(StringUtils.UTF8), request, extraHeaders);
	}
	public final WebResponse createResponse(int code, String codeMessage, String payload, WebRequest request, Pair<String,String>... extraHeaders) {
		return createResponse(code, codeMessage, payload, true, request, extraHeaders);
	}
	public final WebResponse createResponse(int code, String payload, boolean text, WebRequest request, Pair<String,String>... extraHeaders) {
		return createResponse(code, summaryForCode(code), text ? "text/plain" : "text/html", payload.getBytes(StringUtils.UTF8), request, extraHeaders);
	}
	public final WebResponse createResponse(int code, String payload, WebRequest request, Pair<String,String>... extraHeaders) {
		return createResponse(code, summaryForCode(code), payload, true, request, extraHeaders);
	}
	public final WebResponse createResponse(int code, String mimeType, byte[] payload, WebRequest request, Pair<String,String>... extraHeaders) {
		return createResponse(code, summaryForCode(code), mimeType, payload, request, extraHeaders);
	}
	public final WebResponse createResponse(int code, String codeMessage, String mimeType, byte[] payload, WebRequest request, Pair<String,String>... extraHeaders) {
		return createResponse(code, codeMessage, mimeType, payload.length, new ByteArrayInputStream(payload), request, extraHeaders);
	}
	public final WebResponse createResponse(int code, String codeMessage, String mimeType, InputStream payload, WebRequest request, Pair<String,String>... extraHeaders) {
		return createResponse(code, codeMessage, mimeType, -1, payload, request, extraHeaders);
	}
	public final WebResponse createResponse(int code, String mimeType, long size, InputStream payload, WebRequest request, Pair<String,String>... extraHeaders) {
		return createResponse(code, summaryForCode(code), mimeType, size, payload, request, extraHeaders);
	}
	public final WebResponse createResponse(int code, String mimeType, InputStream payload, WebRequest request, Pair<String,String>... extraHeaders) {
		return createResponse(code, summaryForCode(code), mimeType, -1, payload, request, extraHeaders);
	}
	public final WebResponse createResponse(int code, String codeMessage, String mimeType, long size, InputStream payload, WebRequest request, Pair<String,String>... extraHeaders) {
		if(size < 0)
			throw new UnsupportedOperationException();
		
		WebHeaders headers = new WebHeaders();
		for(Pair<String, String> extra : extraHeaders)
			headers.set(extra.i, extra.v);
		headers.set("Content-Type", mimeType);
		if(size > -1)
			headers.set("Content-Length", String.valueOf(size));
		
		String connection = request.headers().get("connection");
		if(connection != null) {
			if(connection.equalsIgnoreCase("keep-alive")) 
				headers.set("Connection", "Keep-Alive");
			else if(connection.equalsIgnoreCase("close")) 
				headers.set("Connection", "close");
			else
				Logger.gears("Unknown connection", connection);
		}
		
		return createResponse(code, codeMessage, headers, payload, request);
	}
	public final WebResponse createResponse(int code, WebHeaders headers, InputStream payload, WebRequest request) {
		return createResponse(code, summaryForCode(code), headers, payload, request);
	}
	public WebResponse createResponse(int code, String codeMessage, WebHeaders headers, InputStream payload, WebRequest request) {
		headers.set("Server", serverName());
		headers.set("Date", dateFormat.format(new Date()));
		return createResponseImpl(code, codeMessage, headers, payload, request);
	}
	protected abstract WebResponse createResponseImpl(int code, String codeMessage, WebHeaders headers, InputStream payload, WebRequest request);
    
    public final WebModule module() {
        return module.get();
    }
    
    public void setModule(WebModule module) {
        this.module.set(module);
    }
    
}
