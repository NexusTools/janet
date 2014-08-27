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
public class PHPFrameworkRequestHandler extends PHPFileRequestHandler {
	
	public PHPFrameworkRequestHandler(String path, String authGET) {
		super(path, authGET);
		install(new Testable<Stream>() {
			public boolean test(Stream against) {
				try {
					return !against.exists() || !against.canRead() || against.isDirectory();
				} catch (IOException ex) {
					return true;
				}
			}
		}, new WebRequestHandler() {
			public WebResponse handle(WebServer server, WebRequest request) throws Throwable {
				return PHPRequestHandler.createResponse(documentRoot, "/index.php", server, request);
			}
		});
	}
	public PHPFrameworkRequestHandler(String path) {
		this(path, null);
	}
	
}
