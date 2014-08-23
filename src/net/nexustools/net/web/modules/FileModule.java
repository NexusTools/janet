/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.web.modules;

/**
 *
 * @author katelyn
 */
public class FileModule extends StreamModule {
	
	public FileModule(String path) {
		super("file:/" + path);
	}
	
}
