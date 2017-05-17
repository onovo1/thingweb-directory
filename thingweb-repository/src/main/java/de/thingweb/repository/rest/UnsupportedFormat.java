package de.thingweb.repository.rest;

import java.io.IOException;

public class UnsupportedFormat extends RESTException {

	@Override
	public int getStatus() {
		return 415;
	}
	
}
