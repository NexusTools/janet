/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web;

import java.io.IOException;

/**
 *
 * @author katelyn
 */
public class CGIException extends IOException {

	public CGIException(String message, Throwable throwable) {
		super(message, throwable);
	}
	
	public CGIException(Throwable throwable) {
		super(throwable);
	}

	public CGIException(String message) {
		super(message);
	}

	public CGIException() {
		super();
	}
	
}
