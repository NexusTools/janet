/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web.http;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.nexustools.data.buffer.basic.StringList;
import net.nexustools.io.InputLineReader;
import net.nexustools.io.LineReader;
import net.nexustools.utils.Pair;
import net.nexustools.utils.log.Logger;
import net.nexustools.web.WebHeaders;

/**
 *
 * @author katelyn
 */
public class HTTPHeaders extends WebHeaders {

	protected static final Pattern headerPattern = Pattern.compile("^([a-z][a-z0-9_\\-]*): (.+)$", Pattern.CASE_INSENSITIVE);
	
	public void parse(InputStream inputStream) throws IOException {
		parse(new InputLineReader(inputStream));
	}
	public void parse(LineReader lineReader) throws IOException {
		String line;
        while(true) {
			Logger.gears(line = lineReader.readNext());
            if(line == null)
                throw new EOFException();
            if(line.trim().length() < 1)
                break;
            
			Matcher matcher = headerPattern.matcher(line);
			if(!matcher.matches())
				throw new IOException("Corrupt header.");
			
			add(matcher.group(1), matcher.group(2).trim());
        }
	}

	static void write(WebHeaders headers, Appendable headerBuilder) throws IOException {
		for(Pair<String,StringList> bundle : headers) {
			for(String value : bundle.v) {
				String key = bundle.i;
				key = key.substring(0, 1).toUpperCase() + key.substring(1);
				int pos = 0;
				while((pos = key.indexOf("-", pos)+1) > 0)
					key = key.substring(0, pos) + key.substring(pos, pos+1).toUpperCase() + key.substring(pos+1);
				headerBuilder.append(key);
				headerBuilder.append(": ");
				headerBuilder.append(value);
				headerBuilder.append("\r\n");
			}
		}
		headerBuilder.append("\r\n");
	}
	
}
