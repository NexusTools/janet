/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.nexustools.data.buffer.basic.StringList;
import net.nexustools.data.buffer.basic.StrongTypeList;
import net.nexustools.janet.Client;
import net.nexustools.utils.ArgumentMap;
import net.nexustools.utils.Pair;
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
	
	protected final StrongTypeList<Runnable> finishers = new StrongTypeList();
	
	public void onFinish(Runnable... finisher) {
		finishers.pushAll(finisher);
	}
	
	public void notifyFinished() {
		for(Runnable finisher : finishers.take())
			finisher.run();
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if(finishers.length() > 0)
			notifyFinished();
	}
    
    public abstract String requestURI();
    public abstract String method();
    public abstract String path();
    
	public abstract WebHeaders headers();
    public abstract ArgumentMap arguments(Scope scope);
	public abstract boolean acceptsEncoding(String encoding);
	
	public String requestString(Scope scope) {
		ArgumentMap map = arguments(scope);
		StringBuilder builder = new StringBuilder();
		
		boolean addAnd = false;
		for(Pair<String, StringList> entry : map) {
			if(entry.v.length() > 0)
				for(String value : entry.v) {
					if(addAnd)
						builder.append('&');
					else
						addAnd = true;
					try {
						builder.append(URLEncoder.encode(entry.i, "UTF-8"));
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
					builder.append(URLEncoder.encode(entry.i, "UTF-8"));
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
