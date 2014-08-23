/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.nexustools.concurrent.Prop;
import net.nexustools.io.Stream;
import net.nexustools.net.Client;
import net.nexustools.net.PacketTransport;
import net.nexustools.net.Server;
import net.nexustools.net.web.modules.WebModule;
import net.nexustools.runtime.RunQueue;
import net.nexustools.utils.Pair;
import net.nexustools.utils.StringUtils;

/**
 *
 * @author kate
 */
public abstract class WebServer<P extends WebPacket, C extends Client<P, ? extends WebServer>> extends Server<P, C> {
	
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    private Prop<WebModule> module;
    public WebServer(WebModule module, int port, Transport protocol, PacketTransport registry, RunQueue runQueue) throws IOException {
        super(port, protocol, registry, runQueue);
        this.module = new Prop(module);
    }
    public WebServer(WebModule module, int port, Transport protocol, RunQueue runQueue) throws IOException {
        super(port, protocol, runQueue);
        this.module = new Prop(module);
    }
    public WebServer(WebModule module, int port, Transport protocol, PacketTransport registry) throws IOException {
        super(port, protocol, registry);
        this.module = new Prop(module);
    }
    public WebServer(WebModule module, int port, Transport protocol) throws IOException {
        super(port, protocol);
        this.module = new Prop(module);
    }
    public WebServer(WebModule module, int port) throws IOException {
        this(module, port, Transport.TCP);
    }
    public WebServer(WebModule module) throws IOException {
        this(module, 80);
    }
	
	protected String serverName() {
		return "JaNET/master";
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
		
		return "???";
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
	
	public final WebResponse streamResponse(Stream stream, String path) throws IOException {
		if(stream.exists())
			if(stream.hasChildren()) {
				if(!path.endsWith("/"))
					path += "/";
				StringBuilder builder = new StringBuilder();
				builder.append("<table><tr><th>Filename</th><th>Size</th></tr>");
				for(String child : stream.children()) {
					Stream childStream = null;
					String childPath = path + child;
					builder.append("<tr");
					try {
						childStream = Stream.open(childPath);
						if(childStream.isHidden())
							builder.append(" class='hidden'");
					} catch(Exception ex) {}
					builder.append("><td><a href='");
					builder.append(childPath);
					builder.append("'>");
					builder.append(child);
					builder.append("</a></td><td>");
					try {
						if(childStream.hasChildren())
							builder.append("Directory");
						else
							builder.append(childStream.sizeStr());
					} catch (Throwable t) {
						builder.append("<error");
						String message = t.getMessage();
						if(message != null) {
							builder.append(" title='");
							builder.append(message);
							builder.append("'");
						}
						builder.append(">");
						builder.append(t.getClass().getSimpleName());
						builder.append("</error>");
					}
					builder.append("</td></tr>");
				}
				builder.append("</table>");

				return systemPage(200, "Index Of: " + path, builder.toString());
			} else
				return createResponse(200, stream.mimeType(), stream.size(), stream.createInputStream());
		else
			return standardResponse(404, "No such file or directory.");
	}
	public final WebResponse streamResponse(String url, String path) throws IOException, URISyntaxException {
		return streamResponse(Stream.open(url), path);
	}
	public final WebResponse streamResponse(Stream stream) throws IOException {
		return streamResponse(stream, stream.path());
	}
	public final WebResponse streamResponse(String url) throws IOException, URISyntaxException {
		return streamResponse(Stream.open(url));
	}
	
	public final WebResponse exceptionResponse(Throwable t) {
		return systemPage(500, t.getClass().getSimpleName(), "<pre>" + StringUtils.stringForException(t) + "</pre>");
	}
	public final WebResponse standardResponse(int code) {
		return standardResponse(code, summaryForCode(code));
	}
	public final WebResponse standardResponse(int code, String codeMessage) {
		return systemPage(code, codeMessage, codeMessage, bodyForCode(code));
	}
	public final WebResponse systemPage(int code, String title, String body) {
		return systemPage(code, summaryForCode(code), title, body);
	}
	public WebResponse systemPage(int code, String codeMessage, String title, String body) {
		StringBuilder html = new StringBuilder();
		html.append("<html><head><title>");
		html.append(title);
		html.append("</title></head><body><h1>");
		html.append(title);
		html.append("</h1>");
		html.append(body);
		html.append("</body></html>");
		
		return createResponse(code, codeMessage, html.toString(), false);
	}
	
	public final WebResponse createResponse(int code, String codeMessage, String payload, boolean text, Pair<String,String>... extraHeaders) {
		return createResponse(code, codeMessage, text ? "text/plain" : "text/html", payload.getBytes(StringUtils.UTF8), extraHeaders);
	}
	public final WebResponse createResponse(int code, String codeMessage, String payload, Pair<String,String>... extraHeaders) {
		return createResponse(code, codeMessage, payload, true, extraHeaders);
	}
	public final WebResponse createResponse(int code, String payload, boolean text, Pair<String,String>... extraHeaders) {
		return createResponse(code, summaryForCode(code), text ? "text/plain" : "text/html", payload.getBytes(StringUtils.UTF8), extraHeaders);
	}
	public final WebResponse createResponse(int code, String payload, Pair<String,String>... extraHeaders) {
		return createResponse(code, summaryForCode(code), payload, true, extraHeaders);
	}
	public final WebResponse createResponse(int code, String mimeType, byte[] payload, Pair<String,String>... extraHeaders) {
		return createResponse(code, summaryForCode(code), mimeType, payload, extraHeaders);
	}
	public final WebResponse createResponse(int code, String codeMessage, String mimeType, byte[] payload, Pair<String,String>... extraHeaders) {
		return createResponse(code, codeMessage, mimeType, payload.length, new ByteArrayInputStream(payload), extraHeaders);
	}
	public final WebResponse createResponse(int code, String codeMessage, String mimeType, InputStream payload, Pair<String,String>... extraHeaders) {
		return createResponse(code, codeMessage, mimeType, -1, payload, extraHeaders);
	}
	public final WebResponse createResponse(int code, String mimeType, long size, InputStream payload, Pair<String,String>... extraHeaders) {
		return createResponse(code, summaryForCode(code), mimeType, size, payload, extraHeaders);
	}
	public final WebResponse createResponse(int code, String mimeType, InputStream payload, Pair<String,String>... extraHeaders) {
		return createResponse(code, summaryForCode(code), mimeType, -1, payload, extraHeaders);
	}
	public final WebResponse createResponse(int code, String codeMessage, String mimeType, long size, InputStream payload, Pair<String,String>... extraHeaders) {
		if(size < 0)
			throw new UnsupportedOperationException();
		
		WebHeaders headers = new WebHeaders();
		for(Pair<String, String> extra : extraHeaders)
			headers.set(extra.i, extra.v);
		headers.set("Content-Type", mimeType);
		headers.set("Content-Length", String.valueOf(size));
		headers.set("Date", dateFormat.format(new Date()));
		headers.set("Server", serverName());
		return createResponse(code, codeMessage, headers, payload);
	}
	public final WebResponse createResponse(int code, WebHeaders headers, InputStream payload) {
		return createResponse(code, summaryForCode(code), headers, payload);
	}
	public abstract WebResponse createResponse(int code, String codeMessage, WebHeaders headers, InputStream payload);
    
    public final WebModule module() {
        return module.get();
    }
    
    public void setModule(WebModule module) {
        this.module.set(module);
    }
    
}
