/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.web.http;

import java.io.IOException;
import java.io.InputStream;
import net.nexustools.io.DataOutputStream;
import net.nexustools.net.Client;
import net.nexustools.net.web.WebHeaders;
import net.nexustools.net.web.WebResponse;
import net.nexustools.utils.StringUtils;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author kate
 */
public class HTTPResponse<T, C extends Client, S extends HTTPServer> extends WebResponse<T, C, S> {
    
    final int status;
    final String statusString;
    final InputStream payload;
    final WebHeaders headers;
    
    public HTTPResponse(int status, String statusString, WebHeaders headers, InputStream payload) {
        this.status = status;
        this.statusString = statusString;
        this.payload = payload;
        this.headers = headers;
    }

    @Override
    public void write(DataOutputStream dataOutput, C client) throws UnsupportedOperationException, IOException {
		String connection = headers.get("connection");
		if(connection == null) {
			Logger.debug("Determining Connection");
			if(headers.has("content-length"))
				connection = "keep-alive";
			else
				connection = "close";
			headers.set("connection", connection);
			Logger.debug(connection);
		}
		boolean close = !connection.equalsIgnoreCase("keep-alive");
		
		Logger.debug("Writing Headers", headers);
        {
            StringBuilder headerBuilder = new StringBuilder();
            headerBuilder.append("HTTP/1.1 ");
            headerBuilder.append(status);
            headerBuilder.append(' ');
            headerBuilder.append(statusString);
            headerBuilder.append("\r\n");
			
			HTTPHeaders.write(headers, headerBuilder);
            dataOutput.write(headerBuilder.toString().getBytes(StringUtils.UTF8));
        }
        
        int read;
        byte[] buffer = new byte[1024 * 1024 * 4]; // 4MB Buffer
        while((read = payload.read(buffer)) > 0)
            dataOutput.write(buffer, 0, read);
		
		if(close) {
			dataOutput.write("\r\n".getBytes(StringUtils.UTF8));
			dataOutput.close();
		} else
			dataOutput.flush();
    }

	@Override
	public int status() {
		return status;
	}

	@Override
	public String statusMessage() {
		return statusString;
	}

	@Override
	public WebHeaders headers() {
		return headers;
	}
	
}
