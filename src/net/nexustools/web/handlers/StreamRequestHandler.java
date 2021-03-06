/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import net.nexustools.data.buffer.basic.StrongTypeMap;
import net.nexustools.io.Stream;
import net.nexustools.utils.Pair;
import net.nexustools.utils.Testable;
import net.nexustools.utils.log.Logger;
import net.nexustools.web.WebRequest;
import net.nexustools.web.WebResponse;
import net.nexustools.web.WebServer;
import static net.nexustools.web.WebServer.dateFormat;

/**
 *
 * @author kate
 */
public class StreamRequestHandler implements WebRequestHandler {
    
	protected final String authGET;
    protected final String documentRoot;
	private final StrongTypeMap<Testable<Stream>, WebRequestHandler> modules = new StrongTypeMap();
    public StreamRequestHandler(String root, String authGET) {
		if(!root.endsWith("/"))
			root += "/";
		this.authGET = authGET;
        documentRoot = root;
    }
    public StreamRequestHandler(String root) {
        this(root, null);
    }
    public StreamRequestHandler() {
        this("run://");
    }
	
	public void install(Testable<Stream> test, WebRequestHandler module) {
		modules.put(test, module);
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
		
		
		WebResponse response = null;
		Stream stream = Stream.open(documentRoot + request.requestURI().substring(1));
		if(stream.isDirectory() && !request.path().endsWith("/"))
			return RedirectRequestHandler.createResponse(request.path() + "/", server, request);
		
		Logger.debug(stream, modules);
		for(Pair<Testable<Stream>, WebRequestHandler> module : modules) {
			try {
				if(!module.i.test(stream))
					continue;
			} catch(Throwable t) {
				Logger.exception(Logger.Level.Gears, t);
				continue;
			}
			try {
				response = module.v.handle(server, request);
			} catch(Throwable t) {
				Logger.exception(Logger.Level.Warning, t);
				response = server.exceptionResponse(t, request);
			}
		}
		
		if(response == null)
			response = createResponse(stream, server, request, authGET);
		
		return response;
    }

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{root=" + documentRoot + ",modules=" + modules + "}";
	}
	
	public static final WebResponse createResponse(Stream stream, WebServer server, WebRequest request) throws IOException {
		return createResponse(stream, server, request, null);
	}
	
	public static final WebResponse createResponse(Stream stream, WebServer server, WebRequest request, String authGET) throws IOException {
		if(!stream.canRead())
			return server.standardResponse(403, request);
		
		if(stream.exists()) {
			if(stream.isDirectory()) {
				String path = request.path();
				if(!path.endsWith("/"))
					path += "/";
				
				StringBuilder builder = new StringBuilder();
				String format = request.headers().get("format", "html");
				
				if(format.equalsIgnoreCase("html"))
					server.directoryListing(builder, stream, request.arguments(WebRequest.Scope.GET_POST), path, authGET);
				else
					throw new UnsupportedOperationException("The requested format is not supported: " + format);
				
				return server.systemPage(200, "Index Of: " + path, builder.toString(), request);
			} else {
				String lastModified = null;
				String mimeType = stream.mimeType();
				try {
					lastModified = dateFormat.format(stream.lastModified());
					String modifiedSince = request.headers().get("If-Modified-Since");
					if(lastModified.equalsIgnoreCase(modifiedSince))
						return server.createResponse(304, mimeType, 0, new InputStream() {
							@Override
							public int read() throws IOException {
								return -1;
							}
						}, request);
				} catch(Throwable t) {}
				
//				final ArgumentMap args = request.arguments(WebRequest.Scope.GET);
//				if(args.containsKey("thumb") && mimeType.startsWith("image/")) {
//					Image image = ImageIO.read(stream.createInputStream());
//					Pair<Float, Float> realSize = new Pair((float)image.getWidth(null), (float)image.getHeight(null));
//					if(24 != realSize.i.intValue() &&
//							24 != realSize.v.intValue()) {
//						Pair<Float, Float> scaleSize = realSize.copy();
//
//						if (scaleSize.i > 24) {
//							scaleSize.v = (24.0f / realSize.i) * realSize.v;
//							scaleSize.i = 24.0f;
//						}
//						if (scaleSize.v > 24) {
//							scaleSize.i = (24.0f / realSize.v) * realSize.i;
//							scaleSize.v = 24.0f;
//						}
//						
//						BufferedImage scaledImage = new BufferedImage(24, 24, BufferedImage.TYPE_USHORT_565_RGB);
//
//						Graphics2D graphics = (Graphics2D)scaledImage.createGraphics();
//						//graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//						// int dx1, int dy1, int dx2, int dy2,
//						//                  int sx1, int sy1, int sx2, int sy2
//						if(Math.round(scaleSize.v) < 24 || Math.round(scaleSize.v) < 24) {
//							graphics.setColor(Color.getColor(args.getArgumentValue("background", "white")));
//							graphics.fillRect(0, 0, 24, 24);
//
//							int x = Math.round(12 - scaleSize.i / 2);
//							int y = Math.round(12 - scaleSize.v / 2);
//							int x2 = Math.round(x + scaleSize.i);
//							int y2 = Math.round(y + scaleSize.v);
//							System.err.println(x + ": " + y + " " + x2 + "," + y2);
//							graphics.drawImage(image, x, y, x2, y2, 0, 0, realSize.i.intValue(), realSize.v.intValue(), null);
//						} else {
//							System.err.println("Scaling 1:1");
//							graphics.drawImage(image, 0, 0, 24, 24, null);
//						}
//
//						ByteArrayOutputStream memStream = new ByteArrayOutputStream();
//						ImageIO.write(scaledImage, "jpeg", memStream);
//
//						if(lastModified != null)
//							return createResponse(200, "image/jpeg", memStream.toByteArray(), request, new Pair<String, String>("Last-Modified", lastModified));
//						return createResponse(200, "image/jpeg", memStream.toByteArray(), request);
//					}
//				}
				
				long size = -1;
				try {
					size = stream.size();
				} catch(IOException ex) {
					Logger.exception(Logger.Level.Gears, ex);
				}
					
				if(lastModified != null)
					return server.createResponse(200, mimeType, size, stream.createInputStream(), request, new Pair<String, String>("Last-Modified", lastModified));
				return server.createResponse(200, mimeType, size, stream.createInputStream(), request);
			}
		} else
			return server.standardResponse(404, "No such file or directory.", request);
	}
	public static final WebResponse createResponse(String url, WebServer server, WebRequest request, String authGET) throws IOException, URISyntaxException {
		return createResponse(Stream.open(url), server, request, authGET);
	}
	public static final WebResponse createResponse(String url, WebServer server, WebRequest request) throws IOException, URISyntaxException {
		return createResponse(Stream.open(url), server, request);
	}
    
}
