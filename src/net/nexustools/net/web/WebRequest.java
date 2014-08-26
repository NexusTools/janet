/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.web;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.nexustools.net.Client;
import net.nexustools.utils.ArgumentMap;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author kate
 */
public abstract class WebRequest<T, C extends Client, S extends WebServer> extends WebPacket<T, C, S> {
	
	public static enum Scope {
		GET,
		POST,
		COOKIE,
		
		GET_POST,
		ALL
	}
    
    public abstract String requestURI();
    public abstract String method();
    public abstract String path();
    
	public abstract WebHeaders headers();
    public abstract ArgumentMap arguments(Scope scope);
	public abstract boolean acceptsEncoding(String encoding);
	
	public abstract void addConnectionListener(ConnectionClosedListener listener);
	
	public String requestString(Scope scope) {
		ArgumentMap map = arguments(scope);
		StringBuilder builder = new StringBuilder();
		
		boolean addAnd = false;
		for(Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
			
			List<String> values = entry.getValue();
			if(values.size() > 0)
				for(String value : values) {
					if(addAnd)
						builder.append('&');
					else
						addAnd = true;
					try {
						builder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
						builder.append('=');
						builder.append(URLEncoder.encode(value, "UTF-8"));
					} catch (UnsupportedEncodingException ex) {
						throw new RuntimeException(ex);
					}
				}
			else {
				if(addAnd)
					builder.append('&');
				else
					addAnd = true;
				
				try {
					builder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
				} catch (UnsupportedEncodingException ex) {
					throw new RuntimeException(ex);
				}
			}
			
			
					
		}
		return "";
	}

    @Override
    protected void recvFromClient(C client, S server) {
		WebResponse response;
        try {
			response = server.module().handle(server, this);
            if(response == null)
				response = server.standardResponse(404, this);
        } catch(Throwable t) {
            response = server.exceptionResponse(t, this);
        }
		Logger.debug("Sending response", response);
		client.send(response);
    }
    
}
