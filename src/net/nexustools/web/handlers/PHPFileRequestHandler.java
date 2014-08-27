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
public class PHPFileRequestHandler extends IndexFileRequestHandler {
	
	public PHPFileRequestHandler(String path, String authGET) {
		super(path, authGET);
		install(new Testable<Stream>() {
			public boolean test(Stream against) {
				return against.extension().equalsIgnoreCase("php");
			}
		}, new WebRequestHandler() {
			public WebResponse handle(WebServer server, WebRequest request) throws Throwable {
				return PHPRequestHandler.createResponse(documentRoot, request.path(), server, request);
			}
		});
		install(new Testable<Stream>() {
			public boolean test(Stream against) {
				try {
					Iterable<String> children = against.children();
					for(String child : children)
						if(child.equalsIgnoreCase("index.php"))
							return true;
				} catch (IOException ex) {}
				return false;
			}
		}, new WebRequestHandler() {
			public WebResponse handle(WebServer server, WebRequest request) throws Throwable {
				return PHPRequestHandler.createResponse(documentRoot,  request.path() + "index.php", server, request);
			}
		});
	}
	public PHPFileRequestHandler(String path) {
		this(path, null);
	}
	/*
		
		
		install(new Testable<Stream>() {
			public boolean test(Stream against) {
				try {
					return !against.exists() || !against.canRead() || against.hasChildren();
				} catch (IOException ex) {
					return true;
				}
			}
		}, new WebRequestHandler() {
			public WebResponse handle(WebServer server, WebRequest request) throws Throwable {
				return PHPRequestHandler.createResponse("/var/www/grapplecorporation",  "/index.php", server, request);
			}
		});
	
	*/
	
}
