/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.web.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.nexustools.net.DisconnectedException;
import net.nexustools.net.web.WebHeaders;
import net.nexustools.utils.Pair;
import net.nexustools.utils.log.Logger;

/**
 *
 * @author katelyn
 */
public class HTTPHeaders extends WebHeaders {

	protected static final Pattern headerPattern = Pattern.compile("^([a-z][a-z0-9_\\-]*): (.+)$", Pattern.CASE_INSENSITIVE);
	
	public void parse(BufferedReader reader) throws IOException {
		String line;
        while(true) {
            line = reader.readLine();
            if(line == null)
                throw new DisconnectedException();
            if(line.trim().length() < 1)
                break;
            
			Matcher matcher = headerPattern.matcher(line);
			if(!matcher.matches())
				throw new IOException("Corrupt header.");
			
			add(matcher.group(1), matcher.group(2));
        }
	}

	static void write(WebHeaders headers, StringBuilder headerBuilder) {
		Logger.debug(headers);
		for(Pair<String,List<String>> bundle : headers) {
			for(String value : bundle.v) {
				headerBuilder.append(bundle.i);
				headerBuilder.append(": ");
				headerBuilder.append(value);
				headerBuilder.append("\r\n");
			}
		}
		headerBuilder.append("\r\n");
	}
	
}
