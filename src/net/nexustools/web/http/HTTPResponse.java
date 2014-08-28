/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web.http;

import java.io.IOException;
import java.io.InputStream;
import net.nexustools.data.buffer.basic.ByteArrayBuffer;
import net.nexustools.io.DataOutputStream;
import net.nexustools.janet.Client;
import net.nexustools.utils.log.Logger;
import net.nexustools.web.WebHeaders;
import net.nexustools.web.WebResponse;
import net.nexustools.web.io.ChunkedEncodingInputStream;

/**
 *
 * @author kate
 */
public class HTTPResponse<T, C extends Client, S extends HTTPServer> extends WebResponse<T, C, S> {
    
    final int status;
    final String statusString;
    final InputStream payload;
    final WebHeaders headers;
    
    public HTTPResponse(int status, String statusString, WebHeaders headers, final InputStream payload, Runnable... finishers) {
		super(finishers);
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
		if(connection == null)
			headers.set("connection", connection = "Keep-Alive");
		
		InputStream payloadReader = payload;
		if(!headers.has("Content-Encoding")) {
			
		}
		
		boolean close = connection.equalsIgnoreCase("close");
		if(!close && Integer.valueOf(headers.get("Content-Length", "0")) < 1 && !headers.has("Transfer-Encoding")) {
			headers.set("Transfer-Encoding", "chunked");
			payloadReader = new ChunkedEncodingInputStream(payloadReader);
		}
		
		Logger.debug("Writing Headers", headers);
        {
            ByteArrayBuffer headerData = new ByteArrayBuffer();
			Appendable headerBuilder = headerData.createAppendable();
            headerBuilder.append("HTTP/1.1 ");
            headerBuilder.append(String.valueOf(status));
            headerBuilder.append(' ');
            headerBuilder.append(statusString);
            headerBuilder.append("\r\n");
			
			HTTPHeaders.write(headers, headerBuilder);
            dataOutput.write(headerData.take());
        }
        
        int read;
        byte[] buffer = new byte[1024 * 1024 * 4]; // 4MB Buffer
        while((read = payloadReader.read(buffer)) > 0)
            dataOutput.write(buffer, 0, read);

		dataOutput.flush();
		if(close)
			dataOutput.close();
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
