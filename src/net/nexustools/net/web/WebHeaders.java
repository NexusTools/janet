/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.nexustools.utils.Pair;

/**
 *
 * @author katelyn
 */
public class WebHeaders implements Iterable<Pair<String, List<String>>> {
	
	protected final HashMap<String, List<String>> headers = new HashMap();
	
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
	public List<String> takes(String key) {
		return headers.remove(key.toLowerCase());
	}
	public List<String> gets(String key) {
		return headers.get(key.toLowerCase());
	}
	public boolean has(String key) {
		return headers.containsKey(key.toLowerCase());
	}
	public void add(String key, String val) {
		List<String> list = headers.get(key.toLowerCase());
		if(list == null)
			headers.put(key.toLowerCase(), list = new ArrayList());
		list.add(val);
	}
	public void set(String key, String val) {
		List<String> list = headers.get(key.toLowerCase());
		if(list == null)
			headers.put(key.toLowerCase(), list = new ArrayList());
		else
			list.clear();
		list.add(val);
	}

	public Iterator<Pair<String, List<String>>> iterator() {
		return new Iterator<Pair<String, List<String>>>() {
			Iterator<Map.Entry<String, List<String>>> it = headers.entrySet().iterator();
			public boolean hasNext() {
				return it.hasNext();
			}
			public Pair<String, List<String>> next() {
				Map.Entry<String, List<String>> entry = it.next();
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
	
}
