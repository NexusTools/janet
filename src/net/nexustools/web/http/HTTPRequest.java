/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.nexustools.event.EventDispatcher;
import net.nexustools.event.ForwardingEventDispatcher;
import net.nexustools.io.DataInputStream;
import net.nexustools.io.InputLineReader;
import net.nexustools.janet.Client;
import net.nexustools.janet.DisconnectedException;
import net.nexustools.web.ConnectionClosedListener;
import net.nexustools.web.ConnectionClosedListener.ConnectionClosedEvent;
import net.nexustools.web.WebHeaders;
import net.nexustools.web.WebRequest;
import net.nexustools.runtime.RunQueue;
import net.nexustools.utils.ArgumentMap;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author kate
 */
public class HTTPRequest<T, C extends Client, S extends HTTPServer> extends WebRequest<T, C, S> {

    private static final Pattern statusLinePattern = Pattern.compile("^([A-Z]{3,10}) (/.*?) (HTTP/\\d\\.\\d)$");
    
    String path;
    String method;
	String rawGET;
	ArgumentMap GET = new ArgumentMap();
	ArgumentMap POST = new ArgumentMap();
	ArgumentMap COOKIE = new ArgumentMap();
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
            throw new DisconnectedException();
        if(line.trim().length() < 1)
            throw new RuntimeException("Missing HTTP Status Line");
        readHeader(line, client);
        
		(headers = new HTTPHeaders()).parse(buffInputStream, reader);
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
				GET.readUrl(rawGET);
			} else
				rawGET = "";
            Logger.debug(method, path, GET);
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
				return GET;
				
			case POST:
				return POST;
				
			case COOKIE:
				return COOKIE;
				
			case GET_POST:
				ArgumentMap combined = new ArgumentMap();
				combined.putAll(GET);
				combined.putAll(POST);
				return combined;
				
			case ALL:
				combined = new ArgumentMap();
				combined.putAll(GET);
				combined.putAll(POST);
				combined.putAll(COOKIE);
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
    
}
