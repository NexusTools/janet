/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web.handlers;

import java.io.IOException;
import net.nexustools.io.Stream;
import net.nexustools.utils.Testable;
import net.nexustools.web.WebRequest;
import net.nexustools.web.WebResponse;
import net.nexustools.web.WebServer;

/**
 *
 * @author katelyn
 */
public class IndexFileRequestHandler extends FileRequestHandler {
	
	public IndexFileRequestHandler(String path, String authGET) {
		super(path, authGET);
		install(new Testable<Stream>() {
			public boolean test(Stream against) {
				try {
					Iterable<String> children = against.children();
					for(String child : children)
						if(child.equalsIgnoreCase("index.html"))
							return true;
				} catch (IOException ex) {}
				return false;
			}
		}, new WebRequestHandler() {
			public WebResponse handle(WebServer server, WebRequest request) throws Throwable {
				return StreamRequestHandler.createResponse(documentRoot + request.path().substring(1) + "index.html", server, request);
			}
		});
		install(new Testable<Stream>() {
			public boolean test(Stream against) {
				try {
					Iterable<String> children = against.children();
					for(String child : children)
						if(child.equalsIgnoreCase("index.htm"))
							return true;
				} catch (IOException ex) {}
				return false;
			}
		}, new WebRequestHandler() {
			public WebResponse handle(WebServer server, WebRequest request) throws Throwable {
				String path = request.path();
				return StreamRequestHandler.createResponse(documentRoot + request.path().substring(1) + "index.htm", server, request);
			}
		});
	}
	public IndexFileRequestHandler(String path) {
		this(path, null);
	}
	
}
