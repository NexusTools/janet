/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.web.handlers;

import net.nexustools.net.web.WebRequest;
import net.nexustools.net.web.WebResponse;
import net.nexustools.net.web.WebServer;

/**
 *
 * @author kate
 */
public interface WebRequestHandler {
    
    public WebResponse handle(WebServer server, WebRequest request) throws Throwable;
    
}
