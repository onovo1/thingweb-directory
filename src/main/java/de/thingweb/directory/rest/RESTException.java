package de.thingweb.directory.rest;

import java.io.IOException;

public class RESTException extends IOException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public int getStatus() {
		return 500;
	}
	
}
