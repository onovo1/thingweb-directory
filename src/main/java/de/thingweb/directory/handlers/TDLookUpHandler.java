package de.thingweb.directory.handlers;

import java.util.List;

import de.thingweb.directory.rest.RESTHandler;
import de.thingweb.directory.rest.RESTServerInstance;

public class TDLookUpHandler extends RESTHandler {

	public TDLookUpHandler(List<RESTServerInstance> instances) {
		super("td-lookup", instances);
	}
	
}
