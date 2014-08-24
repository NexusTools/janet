/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.web;

import java.io.IOException;

/**
 *
 * @author katelyn
 */
public class CGIException extends IOException {

	CGIException(String message, Throwable throwable) {
		super(message, throwable);
	}
	
}
