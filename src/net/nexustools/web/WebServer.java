/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.nexustools.Application;
import net.nexustools.concurrent.Prop;
import net.nexustools.data.buffer.basic.StringList;
import net.nexustools.io.EfficientInputStream;
import net.nexustools.io.Stream;
import net.nexustools.io.StreamUtils;
import net.nexustools.janet.Client;
import net.nexustools.janet.PacketTransport;
import net.nexustools.janet.Server;
import net.nexustools.runtime.RunQueue;
import net.nexustools.utils.ArgumentMap;
import net.nexustools.utils.Pair;
import net.nexustools.utils.StringUtils;
import net.nexustools.utils.log.Logger;
import net.nexustools.web.handlers.WebRequestHandler;

/**
 *
 * @author kate
 */
public abstract class WebServer<P extends WebPacket, C extends Client<P, ? extends WebServer>> extends Server<P, C> {
	
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    private Prop<WebRequestHandler> module;
	protected final StringList systemStyles = new StringList();
	protected final StringList systemScripts = new StringList();
    public WebServer(WebRequestHandler module, int port, Protocol protocol, PacketTransport registry, RunQueue runQueue, Object... args) throws IOException {
        super(port, protocol, registry, runQueue, args);
        this.module = new Prop(module);
		init();
    }
    public WebServer(WebRequestHandler module, int port, Protocol protocol, RunQueue runQueue, Object... args) throws IOException {
        super(port, protocol, runQueue, args);
        this.module = new Prop(module);
		init();
    }
    public WebServer(WebRequestHandler module, int port, Protocol protocol, PacketTransport registry, Object... args) throws IOException {
        super(port, protocol, registry, args);
        this.module = new Prop(module);
		init();
    }
    public WebServer(WebRequestHandler module, int port, Protocol protocol, Object... args) throws IOException {
        super(port, protocol, args);
        this.module = new Prop(module);
		init();
    }
    public WebServer(WebRequestHandler module, int port) throws IOException {
        this(module, port, Protocol.TCP);
    }
    public WebServer(WebRequestHandler module) throws IOException {
        this(module, 80);
    }
	
	protected void init() {
		systemStyles.push("resource:/net/nexustools/web/System.css");
	}
	
	public String serverName() {
		return Application.name();
	}
	
	public String summaryForCode(int code) {
		switch(code) {
			case 200:
				return "OK";
				
			case 301:
				return "Moved Permanently";
				
			case 302:
				return "Found";
				
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
	
	public String bodyForCode(int code) {
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
	
	public void listingHTMLContent(StringBuilder builder, Stream stream, ArgumentMap arguments, String path, String authGET) throws IOException {
		builder.append("<table><tr><th></th><th>Filename</th><th>Type</th><th>Last Modified</th><th>Size</th></tr>");
		
		{
			String parent = path;
			if(parent.endsWith("/"))
				parent = parent.substring(0, parent.length()-1);
			else
				path += "/";
			int pos = parent.lastIndexOf("/");
			if(pos > -1) {
				parent = parent.substring(0, pos+1);
				builder.append("<tr><td></td><td style=\"width: 100%\"><a href=\"");
				builder.append(parent);
				if(authGET != null) {
					builder.append('?');
					builder.append(authGET);
				}
				builder.append("\">..</a></td><td>inode/directory</td><td>..</td><td>..</td></tr>");
			}
		}
		
		
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
			if(preview) {
				builder.append("<object width=\"24\" height=\"24\" data=\"");
				builder.append(childPath);
				if(authGET != null) {
					builder.append('?');
					builder.append(authGET);
					builder.append('&');
				} else
					builder.append('?');
				builder.append("thumb\" type=\"");
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
			if(authGET != null) {
				builder.append('?');
				builder.append(authGET);
			}
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
	
	public final WebResponse exceptionResponse(Throwable t, WebRequest request) {
		Logger.exception(Logger.Level.Debug, t);
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
	public final static byte[] LR = "\r\n".getBytes(StringUtils.UTF8);
	public final static byte[] ChunkEnd = "0\r\n\r\n".getBytes(StringUtils.UTF8);
	public final WebResponse createResponse(int code, String codeMessage, String mimeType, final long size, InputStream payload, WebRequest request, Pair<String,String>... extraHeaders) {
		WebHeaders headers = new WebHeaders();
		for(Pair<String, String> extra : extraHeaders)
			headers.set(extra.i, extra.v);
		headers.set("Content-Type", mimeType);
		if(size > -1)
			headers.set("Content-Length", String.valueOf(size));
		
		boolean keepAlive = false;
		String connection = request.headers().get("connection");
		if(connection != null) {
			if(connection.equalsIgnoreCase("keep-alive")) { 
				headers.set("Connection", "Keep-Alive");
				keepAlive = true;
			} else if(connection.equalsIgnoreCase("close")) 
				headers.set("Connection", "close");
			else
				Logger.gears("Unknown connection", connection);
		}
		
		String range = request.headers().take("range");
		if(range != null) // TODO: Implement returning a range
			return standardResponse(501, request);
		
//		if(mimeType.startsWith("text/") && !headers.has("Content-Encoding")) {
//			if(request.acceptsEncoding("gzip")) {
//				final InputStream iStream = payload;
//				headers.set("Content-Encoding", "gzip");
//				System.err.println("Starting GZIP Output");
//				try {
//					payload = new EfficientInputStream() {
//						int pos = 0;
//						int outSize = 0;
//						boolean finished = false;
//						byte[] inBuffer = new byte[StreamUtils.DefaultCopySize];
//						byte[] outBuffer = new byte[StreamUtils.DefaultCopySize];
//						final GZIPOutputStream outStream = new GZIPOutputStream(new EfficientOutputStream() {
//							@Override
//							public void write(byte[] b, int off, int len) throws IOException {
//								System.err.println("Writing GZIP Stream " + len);
//								System.arraycopy(b, off, outBuffer, outSize, len);
//								outSize += len;
//							}
//						});
//						@Override
//						public int read(byte[] b, int off, int len) throws IOException {
//							int read;
//							if(!finished && pos < 1) {
//								read = Math.min(len, StreamUtils.DefaultCopySize);
//								System.err.println("Reading " + read + "bytes");
//								read = iStream.read(inBuffer, 0, read);
//								System.err.println("Read " + read + "bytes");
//								if(read == 0)
//									return 0;
//
//								if(read < 0) {
//									System.err.println("Finishing GZIP Stream");
//									outStream.close();
//									finished = true;
//								} else {
//									System.err.println("Writing GZIP Stream");
//									outStream.write(inBuffer, 0, read);
//								}
//							}
//
//							read = Math.min(len, outSize-pos);
//							System.err.println("Reading " + read + " GZipped Bytes");
//							if(read > 0) {
//								System.arraycopy(outBuffer, pos, b, off, read);
//								System.err.println("Read " + read + " GZipped Bytes");
//								pos += read;
//								if(pos >= outSize) {
//									System.err.println("Reset GZip Buffer");
//									outSize = 0;
//									pos = 0;
//								}
//								return read;
//							}
//							System.err.println("Done Reading");
//							return -1;
//						}
//					};
//				} catch (IOException ex) {
//					throw new RuntimeException(ex);
//				}
//			}
//		}
		
		return createRawResponse(code, codeMessage, headers, payload, request);
	}
	public final WebResponse createRawResponse(int code, WebHeaders headers, InputStream payload, WebRequest request) {
		return createRawResponse(code, summaryForCode(code), headers, payload, request);
	}
	public WebResponse createRawResponse(int code, String codeMessage, WebHeaders headers, InputStream payload, WebRequest request) {
		headers.set("Server", serverName());
		headers.set("Date", dateFormat.format(new Date()));
		
		if(request.method().equalsIgnoreCase("head"))
			payload = new InputStream() {
				@Override
				public int read() throws IOException {
					return -1;
				}
			};
		else {
			if(Integer.valueOf(headers.get("Content-Length", "0")) <= 0 && !headers.has("Transfer-Encoding")) {
				headers.set("Transfer-Encoding", "chunked");
				headers.set("Connection", "keep-alive");
				final InputStream iStream = payload;
				payload = new EfficientInputStream() {
					byte[] tempBuffer = new byte[StreamUtils.DefaultMaxCopySize];
					boolean endOfStream = false;
					@Override
					public int read(byte[] b, int off, int len) throws IOException {
						if(endOfStream)
							return -1;
						
						int read = Math.min(tempBuffer.length, len-20);
						read = iStream.read(tempBuffer, 0, read);
						if(read < 1) {
							if(read < 0) {
								System.arraycopy(ChunkEnd, 0, b, off, ChunkEnd.length);
								endOfStream = true;
								return ChunkEnd.length;
							}
							return read;
						}
						
						byte[] chunkBytes = (Integer.toHexString(read) + "\r\n").getBytes(StringUtils.UTF8);
						System.arraycopy(chunkBytes, 0, b, off, chunkBytes.length);
						System.arraycopy(tempBuffer, 0, b, off+chunkBytes.length, read);
						System.arraycopy(LR, 0, b, off+chunkBytes.length+read, LR.length);
						return read + chunkBytes.length + LR.length;
					}
					@Override
					public void close() throws IOException {
						iStream.close();
						super.close();
					}
				};
			}
		}
		
		
		return createResponseImpl(code, codeMessage, headers, payload, request);
	}
	protected abstract WebResponse createResponseImpl(int code, String codeMessage, WebHeaders headers, InputStream payload, WebRequest request);
    
    public final WebRequestHandler module() {
        return module.get();
    }
    
    public void setModule(WebRequestHandler module) {
        this.module.set(module);
    }

	public WebResponse tryHandle(WebServer server, WebRequest request, Iterable<WebRequestHandler> modules, WebRequestHandler def) {
		WebResponse response = tryHandle(server, request, modules);
		if(response == null && def != null)
			try {
				response = def.handle(server, request);
			} catch (Throwable ex) {
				response = exceptionResponse(ex, request);
			}
		return response;
	}
	public WebResponse tryHandle(WebServer server, WebRequest request, Iterable<WebRequestHandler> modules) {
		WebResponse response = null;
		for(WebRequestHandler module : modules) {
			try {
				WebResponse newResponse = module.handle(server, request);
				if(newResponse != null) {
					if(newResponse.status() == 200) {
						response = newResponse;
						break;
					} else if(response == null)
						response = newResponse;
				}
			} catch(Throwable t) {
				if(response == null)
					response = server.exceptionResponse(t, request);
			}
		}
		return response;
	}
    
}
