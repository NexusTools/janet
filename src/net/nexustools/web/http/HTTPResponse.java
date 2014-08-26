/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

import net.nexustools.io.DataOutputStream;
import net.nexustools.janet.Client;
import net.nexustools.web.WebHeaders;
import net.nexustools.web.WebResponse;
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
    
    public HTTPResponse(int status, String statusString, WebHeaders headers, final InputStream payload) {
        this.status = status;
        this.statusString = statusString;
        this.payload = payload;
        this.headers = headers;
		
		onFinish(new Runnable() {
			public void run() {
				try {
					payload.close();
				} catch (IOException ex) {}
			}
		});
    }

    @Override
    public void write(DataOutputStream dataOutput, C client) throws UnsupportedOperationException, IOException {
		String connection = headers.get("connection");
		//if(connection == null)
			headers.set("connection", connection = "close");
		
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
        payload.close();

		//if(close)
			dataOutput.close();
		//else {
		//	dataOutput.write(WebServer.LR);
		//	dataOutput.flush();
		//}
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
