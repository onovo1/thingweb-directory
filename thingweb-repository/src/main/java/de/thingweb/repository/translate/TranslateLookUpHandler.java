package de.thingweb.repository.translate;

import java.net.URI;
import java.util.List;
import java.util.Map;

import de.thingweb.repository.rest.BadRequestException;
import de.thingweb.repository.rest.RESTException;
import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTResource;
import de.thingweb.repository.rest.RESTServerInstance;

public class TranslateLookUpHandler extends RESTHandler {

	public TranslateLookUpHandler(List<RESTServerInstance> instances) {
		super("translate-lookup", instances);
	}
	
	@Override
	public RESTResource get(URI uri, Map<String, String> parameters) throws RESTException {

		RESTResource resource = new RESTResource(name(uri), this);
		resource.contentType = "application/ld+json";
		String output = "";
		String source = "";
		String target = "";
		String rt	  = "";
		
		resource.content = "{";
		
		// GET source and target TD IDs from query parameters
		if (parameters.containsKey("source") && !parameters.get("source").isEmpty() && parameters.containsKey("target")
				&& !parameters.get("target").isEmpty()&& parameters.containsKey("rt") && !parameters.get("rt").isEmpty()) {
			source = TranslateUtils.getid(parameters.get("source"));
			target = TranslateUtils.getid(parameters.get("target"));
			rt 	   = parameters.get("rt");
		} else {
			throw new BadRequestException();
		}
		
		String id = source + "_" + target + "_" + rt;
		
		output = TranslateUtils.getTranslateFromId(uri, id);
		
		// If it's empty add it into the failed lookup list
		if (output.isEmpty()) {
			TranslateFailLookUpHandler.addEntry(uri, id);		
		}
		
		resource.content = output;
						
		resource.content += "}";	
		return resource;
	}
	
	private String name(URI uri) {
		
		String path = uri.getPath();
		if (path.contains("/")) {
			return path.substring(uri.getPath().lastIndexOf("/") + 1);
		}
		return path;
	}
}
