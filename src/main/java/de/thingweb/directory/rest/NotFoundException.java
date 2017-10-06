package de.thingweb.directory.rest;

public class NotFoundException extends RESTException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public int getStatus() {
		return 404;
	}
	
}
