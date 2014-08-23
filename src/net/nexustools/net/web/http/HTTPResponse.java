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
    }
	
}
