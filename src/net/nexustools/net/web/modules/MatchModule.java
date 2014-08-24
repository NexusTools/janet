/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.web.modules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import net.nexustools.net.web.WebRequest;
import net.nexustools.net.web.WebResponse;
import net.nexustools.net.web.WebServer;
import net.nexustools.utils.Testable;

/**
 *
 * @author katelyn
 */
public class MatchModule implements WebModule {
	
	final WebModule def;
	final LinkedHashMap<Testable<WebRequest>, WebModule> moduleMap = new LinkedHashMap();
	public MatchModule(WebModule def) {
		this.def = def;
	}
	public MatchModule() {
		this(null);
	}
	
	public void add(Testable<WebRequest> matcher, WebModule module) {
		moduleMap.put(matcher, module);
	}
	public void remove(Testable<WebRequest> matcher) {
		moduleMap.remove(matcher);
	}

	public WebResponse handle(WebServer server, WebRequest request) throws Throwable {
		ArrayList<WebModule> modules = new ArrayList();
		for(Map.Entry<Testable<WebRequest>, WebModule> entry : moduleMap.entrySet())
			if(entry.getKey().test(request))
				modules.add(entry.getValue());
		
		return server.tryHandle(server, request, modules, def);
	}
	
}
