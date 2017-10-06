package de.thingweb.directory.rest;

public class MethodNotAllowedException extends RESTException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public int getStatus() {
		return 405;
	}
	
}
