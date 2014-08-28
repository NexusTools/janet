/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web;

import net.nexustools.io.Stream;
import net.nexustools.io.TemporaryFileStream;
import net.nexustools.utils.ArgumentMap;

/**
 *
 * @author katelyn
 */
public class SimpleWebRequest extends WebRequest {
	
	private final String path;
	private final String method;
	private final WebHeaders headers;
	private final ArgumentMap args = new ArgumentMap();
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
		return method;
	}

	@Override
	public String path() {
		return path;
	}

	@Override
	public WebHeaders headers() {
		return headers;
	}

	@Override
	public String requestURI() {
		String get = requestString(Scope.GET);
		if(get == null)
			return path;
		return path + '?' + get;
	}

	@Override
	public ArgumentMap arguments(Scope scope) {
		return args;
	}

	@Override
	public boolean acceptsEncoding(String encoding) {
		return false;
	}

	@Override
	public long payloadLength() {
		return 0;
	}

	@Override
	public String payloadType() {
		return null;
	}

	@Override
	public Stream payload() {
		return Stream.Void();
	}

	@Override
	public Stream payloadFile(String name) {
		return null;
	}
	
}
