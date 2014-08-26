/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web.handlers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import net.nexustools.web.WebRequest;
import net.nexustools.web.WebResponse;
import net.nexustools.web.WebServer;
import net.nexustools.utils.Testable;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author katelyn
 */
public class MatchRequestHandler implements WebRequestHandler {
	
	final WebRequestHandler def;
	final LinkedHashMap<Testable<WebRequest>, WebRequestHandler> moduleMap = new LinkedHashMap();
	public MatchRequestHandler(WebRequestHandler def) {
		this.def = def;
	}
	public MatchRequestHandler() {
		this(null);
	}
	
	public void add(Testable<WebRequest> matcher, WebRequestHandler module) {
		moduleMap.put(matcher, module);
	}
	public void remove(Testable<WebRequest> matcher) {
		moduleMap.remove(matcher);
	}

	public WebResponse handle(WebServer server, WebRequest request) throws Throwable {
		ArrayList<WebRequestHandler> modules = new ArrayList();
		for(Map.Entry<Testable<WebRequest>, WebRequestHandler> entry : moduleMap.entrySet())
			try {
				if(entry.getKey().test(request))
					modules.add(entry.getValue());
			} catch(Throwable t) {
				Logger.exception(Logger.Level.Gears, t);
			}
		
		return server.tryHandle(server, request, modules, def);
	}
	
}
