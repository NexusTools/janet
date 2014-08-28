/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.nexustools.data.annote.ThreadUnsafe;
import net.nexustools.data.buffer.basic.StringList;
import net.nexustools.utils.Pair;

/**
 *
 * @author katelyn
 */
@ThreadUnsafe
public class WebHeaders implements Iterable<Pair<String, StringList>> {
	
	protected final HashMap<String, StringList> headers = new HashMap();
	
	public String get(String key) {
		try {
			return gets(key.toLowerCase()).get(0);
		} catch(IndexOutOfBoundsException ex) {
			return null;
		} catch(NullPointerException ex) {
			return null;
		}
	}
	public String get(String key, String def) {
		String val = null;
		try {
			val = gets(key.toLowerCase()).get(0);
		} catch(IndexOutOfBoundsException ex) {
		} catch(NullPointerException ex) {}
		return val == null ? def : val;
	}
	public String take(String key) {
		try {
			return takes(key.toLowerCase()).get(0);
		} catch(IndexOutOfBoundsException ex) {
			return null;
		} catch(NullPointerException ex) {
			return null;
		}
	}
	public StringList takes(String key) {
		return headers.remove(key);
	}
	public StringList gets(String key) {
		return headers.get(key.toLowerCase());
	}
	public boolean has(String key) {
		return headers.containsKey(key.toLowerCase());
	}
	public void add(String key, String val) {
		StringList list = headers.get(key.toLowerCase());
		if(list == null)
			headers.put(key.toLowerCase(), list = new StringList());
		list.push(val);
	}
	public void set(String key, String val) {
		StringList list = headers.get(key.toLowerCase());
		if(list == null)
			headers.put(key.toLowerCase(), new StringList(val));
		else {
			list.clear();
			list.push(val);
		}
	}

	public Iterator<Pair<String, StringList>> iterator() {
		return new Iterator<Pair<String, StringList>>() {
			Iterator<Map.Entry<String, StringList>> it = headers.entrySet().iterator();
			public boolean hasNext() {
				return it.hasNext();
			}
			public Pair<String, StringList> next() {
				Map.Entry<String, StringList> entry = it.next();
				return new Pair(entry.getKey(), entry.getValue());
			}
			public void remove() {
				throw new UnsupportedOperationException("Not supported yet.");
			}
		};
	}

	@Override
	public String toString() {
		return headers.toString();
	}

	public void remove(String key) {
		headers.remove(key);
	}
	
}
