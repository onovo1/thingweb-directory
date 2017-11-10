package de.thingweb.directory.translate;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTHandler;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.rest.RESTServerInstance;

import org.json.JSONObject;

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
		
		//resource.content = "{";
		resource.content = "";
		
		// Single request of a Translation
		// GET source and target TD IDs from query parameters
		if (parameters.containsKey("source") && !parameters.get("source").isEmpty() && parameters.containsKey("target")
				&& !parameters.get("target").isEmpty()&& parameters.containsKey("rt") && !parameters.get("rt").isEmpty()) {
			source = TranslateUtils.getid(parameters.get("source"));
			target = TranslateUtils.getid(parameters.get("target"));
			rt 	   = parameters.get("rt");
			
			String id = source + "_" + target + "_" + rt;
			
			output = TranslateUtils.getTranslateFromId(uri, id);
			
			// If it's empty add it into the failed lookup list
			if (output.isEmpty()) {
				//Create an instance of the class
				TranslateFailLookUpHandler translateFailLookUpHandler = new TranslateFailLookUpHandler(instances);
				
				translateFailLookUpHandler.addEntry(uri, id);		
			}
			
		    resource.content = output;
							
			//resource.content += "}";
			

	    // List of the Translations of the database
		} else if (parameters.isEmpty()){
			
			List<String> translations = new ArrayList<String>();
					
			// Return all Translations
			try {
				translations = TranslateUtils.listTranslations("/translate/");
			} catch (Exception e) {
				throw new BadRequestException(null);
			}
			
			JSONObject root = new JSONObject();
			for(String translation: translations){
				
			    URI translationUri = URI.create(translation);
			    
			    translation = translationUri.getPath();
			    String id = translation.substring(translation.lastIndexOf("/")+1);
			    
	            List<String> entries = Arrays.asList(id.split("_"));
	            JSONObject obj = TranslateUtils.createObject(entries.get(0), entries.get(1), entries.get(2));
				root.put(translation, obj);
			}
			resource.content = root.toString();
			
		} else {
			throw new BadRequestException(null);
		}
			
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
