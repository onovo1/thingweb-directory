package de.thingweb.directory.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import de.thingweb.directory.ThingDescriptionUtils;
import de.thingweb.directory.rest.NotFoundException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTHandler;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.rest.RESTServerInstance;

public class OpenAPISpecHandler extends RESTHandler {
	
	public static final String FILENAME = "api.json";
	
	private static String sSpec;

	public OpenAPISpecHandler(List<RESTServerInstance> instances) {
		super(FILENAME, instances);
		
		if (sSpec == null) {
			// load at first instantiation
			sSpec = loadFile(FILENAME);
		}
	}

	@Override
	public RESTResource get(URI uri, Map<String, String> parameters) throws RESTException {
		if (sSpec != null) {
		    RESTResource spec = new RESTResource(FILENAME, this);
		    spec.content = sSpec;
		    spec.contentType = "application/json";
		    return spec;
		} else {
			throw new NotFoundException();
		}
	}
	
	private String loadFile(String filename) {
        InputStream in = getClass().getClassLoader().getResourceAsStream(filename);

        try {
	        return ThingDescriptionUtils.streamToString(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
		return null;
	}
	
}
