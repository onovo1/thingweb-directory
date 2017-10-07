package de.thingweb.directory.rest;

public class UnsupportedFormat extends RESTException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public int getStatus() {
		return 415;
	}
	
}
