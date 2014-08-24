/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.web;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import net.nexustools.net.Client;
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
    
    public abstract String method();
    public abstract String path();
    
	public abstract WebHeaders headers();
    public abstract Map<String,String> request(Scope scope);
	
	public String requestString(Scope scope) {
		Map<String,String> map = request(scope);
		StringBuilder builder = new StringBuilder();
		
		boolean addAnd = false;
		for(Map.Entry<String, String> entry : map.entrySet()) {
			if(addAnd)
				builder.append('&');
			else
				addAnd = true;
			
			try {
				builder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
				builder.append('=');
				builder.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
			} catch (UnsupportedEncodingException ex) {
				throw new RuntimeException(ex);
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
