/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web;

import net.nexustools.utils.ArgumentMap;

/**
 *
 * @author katelyn
 */
public class SimpleWebRequest extends WebRequest {
	
	private final String path;
	private final String method;
	private final WebHeaders headers;
	public SimpleWebRequest(String path, WebHeaders headers) {
		this("GET", path, headers);
	}
	public SimpleWebRequest(String path) {
		this("GET", path, new WebHeaders());
	}
	public SimpleWebRequest(String method, String path, WebHeaders headers) {
		this.headers = headers;
		this.method = method;
		this.path = path;
	}

	@Override
	public String method() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String path() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public WebHeaders headers() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String requestURI() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public ArgumentMap arguments(Scope scope) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean acceptsEncoding(String encoding) {
		return false;
	}
	
}
