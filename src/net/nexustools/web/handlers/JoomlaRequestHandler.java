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
public class JoomlaRequestHandler extends PHPFrameworkRequestHandler {
	
	public JoomlaRequestHandler(String path, String authGET) {
		super(path, authGET);
		install(new Testable<Stream>() {
			public boolean test(Stream against) {
				try {
					return against.path().startsWith(documentRoot + "administrator/") && (!against.exists() || !against.canRead() || against.isDirectory());
				} catch (IOException ex) {
					return true;
				}
			}
		}, new WebRequestHandler() {
			public WebResponse handle(WebServer server, WebRequest request) throws Throwable {
				return PHPRequestHandler.createResponse(documentRoot, "/administrator/index.php", server, request);
			}
		});
	}
	public JoomlaRequestHandler(String path) {
		this(path, null);
	}
	
}
