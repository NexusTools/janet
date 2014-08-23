/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.web.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.nexustools.io.DataInputStream;
import net.nexustools.net.Client;
import net.nexustools.net.DisconnectedException;
import net.nexustools.net.web.WebHeaders;
import net.nexustools.net.web.WebRequest;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author kate
 */
public class HTTPRequest<T, C extends Client, S extends HTTPServer> extends WebRequest<T, C, S> {

    private static final Pattern statusLinePattern = Pattern.compile("^([A-Z]{3,10}) (/.*?) (HTTP/\\d\\.\\d)$");
    
    String path;
    String method;
	HTTPHeaders headers;

	@Override
	public void read(DataInputStream dataInput, C client) throws UnsupportedOperationException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(dataInput));
        String line = reader.readLine();
        if(line == null)
            throw new DisconnectedException();
        if(line.trim().length() < 1)
            throw new RuntimeException("Missing HTTP Status Line");
        readHeader(line, client);
        
		(headers = new HTTPHeaders()).parse(reader);
	}

//    @Override
    protected void readHeader(String statusLine, C client) throws UnsupportedOperationException, IOException {
        Matcher matcher = statusLinePattern.matcher(statusLine);
        if(!matcher.matches())
            throw new IOException("Invalid request status line.");
        else {
            method = matcher.group(1);
            path = matcher.group(2);
            Logger.debug(method, path);
        }
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
	public Map<String, String> request(Scope scope) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
    
}
