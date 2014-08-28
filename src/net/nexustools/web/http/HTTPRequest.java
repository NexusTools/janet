/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web.http;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.nexustools.data.buffer.basic.StrongTypeMap;
import net.nexustools.event.ForwardingEventDispatcher;
import net.nexustools.io.DataInputStream;
import net.nexustools.io.InputLineReader;
import net.nexustools.io.Stream;
import net.nexustools.io.StreamUtils;
import net.nexustools.io.TemporaryFileStream;
import net.nexustools.janet.Client;
import net.nexustools.runtime.RunQueue;
import net.nexustools.utils.ArgumentMap;
import net.nexustools.utils.NXUtils;
import net.nexustools.utils.Pair;
import net.nexustools.utils.StringUtils;
import net.nexustools.utils.log.Logger;
import net.nexustools.web.ConnectionClosedListener;
import net.nexustools.web.ConnectionClosedListener.ConnectionClosedEvent;
import net.nexustools.web.WebHeaders;
import net.nexustools.web.WebRequest;

/**
 *
 * @author kate
 */
public class HTTPRequest<T, C extends Client, S extends HTTPServer> extends WebRequest<T, C, S> {

    private static final Pattern statusLinePattern = Pattern.compile("^([A-Z]{3,10}) (/.*?) (HTTP/\\d\\.\\d)$");
    
    String path;
    String method;
	String rawGET;
	ArgumentMap GET;
	ArgumentMap POST;
	ArgumentMap COOKIE;
	Pair<String, Stream> payload;
	StrongTypeMap<String, Stream> FILES = new StrongTypeMap();
	HTTPHeaders headers;
	
	final ForwardingEventDispatcher<?, ConnectionClosedListener, ConnectionClosedEvent> connectionDispatcher;
	public HTTPRequest(RunQueue runQueue) {
		connectionDispatcher = new ForwardingEventDispatcher(runQueue);
	}

	@Override
	public void read(DataInputStream dataInput, C client) throws UnsupportedOperationException, IOException {
        BufferedInputStream buffInputStream = new BufferedInputStream(dataInput);
		InputLineReader reader = new InputLineReader(buffInputStream);
        String line = reader.readNext();
        if(line == null)
            throw new EOFException();
        if(line.trim().length() < 1)
            throw new RuntimeException("Missing HTTP Status Line");
        readHeader(line, client);
        
		(headers = new HTTPHeaders()).parse(reader);
		if(headers.has("Content-Length")) {
			long length = Long.valueOf(headers.get("Content-Length"));
			final TemporaryFileStream requestPayload = new TemporaryFileStream();
			OutputStream out = requestPayload.createOutputStream();
			try {
				StreamUtils.copy(buffInputStream, out, length);
				payload = new Pair(headers.get("Content-Type"), requestPayload);
				onFinish(new Runnable() {
					public void run() {
						requestPayload.delete();
					}
				});
			} finally {
				out.close();
			}
		} else
			payload = new Pair(null, Stream.Void());
	}

	@Override
	public String requestURI() {
		if(rawGET.length() > 0)
			return path + "?" + rawGET;
		return path;
	}

	@Override
	public String requestString(Scope scope) {
		switch(scope) {
			case GET:
				return rawGET;
				
			default:
				return super.requestString(scope);
		}
	}
	

//    @Override
    protected void readHeader(String statusLine, C client) throws UnsupportedOperationException, IOException {
        Matcher matcher = statusLinePattern.matcher(statusLine);
        if(!matcher.matches())
            throw new IOException("Invalid request status line.");
        else {
            method = matcher.group(1);
            path = matcher.group(2);
			int argPos = path.indexOf("?");
			if(argPos > 0) {
				rawGET = path.substring(argPos + 1);
				path = path.substring(0, argPos);
			} else
				rawGET = "";
            Logger.debug(method, path);
        }
    }

	@Override
	public String method() {
		return method;
	}

	@Override
	public String path() {
		return path;
	}

	@Override
	public WebHeaders headers() {
		return headers;
	}

	@Override
	public ArgumentMap arguments(Scope scope) {
		switch(scope) {
			case GET:
				if(GET == null) {
					GET = new ArgumentMap();
					try {
						GET.readURLEncoded(rawGET);
					} catch (UnsupportedEncodingException ex) {
						throw NXUtils.wrapRuntime(ex);
					}
				}
				return GET;
				
			case POST:
				if(POST == null) {
					POST = new ArgumentMap();
					if(payload.i != null && payload.i.equalsIgnoreCase("application/x-www-form-urlencoded"))
						try {
							if(payload.i.length() > 4096)
								throw new IOException("Limit of 4KB for GET request payload.");
							
							GET.readURLEncoded(StringUtils.readUTF8(payload.v.createInputStream()));
						} catch (UnsupportedEncodingException ex) {
							throw NXUtils.wrapRuntime(ex);
						} catch (IOException ex) {
							throw NXUtils.wrapRuntime(ex);
						}
				}
				return POST;
				
			case COOKIE:
				if(COOKIE == null) {
					COOKIE = new ArgumentMap();
					// TODO: Parse cookie headers
				}
				return COOKIE;
				
			case GET_POST:
				ArgumentMap combined = new ArgumentMap();
				combined.putAll(arguments(Scope.GET));
				combined.putAll(arguments(Scope.POST));
				return combined;
				
			case ALL:
				combined = new ArgumentMap();
				combined.putAll(arguments(Scope.GET));
				combined.putAll(arguments(Scope.POST));
				combined.putAll(arguments(Scope.COOKIE));
				return combined;
		}
		
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean acceptsEncoding(String request) {
		try {
			for(String encoding : headers().get("Accept-Encoding").split(","))
				if(encoding.equalsIgnoreCase(request))
					return true;
		} catch(Throwable t) {
			Logger.exception(Logger.Level.Gears, t);
		}
			
		throw new UnsupportedOperationException();
	}

	@Override
	public long payloadLength() {
		try {
			return payload.v.size();
		} catch (IOException ex) {
			throw NXUtils.wrapRuntime(ex);
		}
	}

	@Override
	public String payloadType() {
		return payload.i;
	}

	@Override
	public Stream payload() {
		return payload.v;
	}

	@Override
	public Stream payloadFile(String name) {
		if(FILES == null) // TODO: Parse file streams out of the payload
			throw new UnsupportedOperationException();
		return FILES.get(name);
	}
    
}
