/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web.handlers;

/**
 *
 * @author katelyn
 */
public class FileRequestHandler extends StreamRequestHandler {
	
	public FileRequestHandler(String path, String authGET) {
		super("file://" + path, authGET);
	}
	public FileRequestHandler(String path) {
		this("file://" + path, null);
	}
	
}
