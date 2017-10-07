package de.thingweb.directory.rest;

public class BadRequestException extends RESTException {
	
	String path = null;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BadRequestException(String uri){
		path = uri;
	}

	public String getPath(){
		return path;
	}

	@Override
	public int getStatus() {
		return 400;
	}
	
}
