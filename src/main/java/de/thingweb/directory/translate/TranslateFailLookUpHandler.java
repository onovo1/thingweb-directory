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

public class TranslateFailLookUpHandler extends RESTHandler {

	public TranslateFailLookUpHandler(List<RESTServerInstance> instances) {
		super("fail", instances);
	}

	@Override
	public RESTResource get(URI uri, Map<String, String> parameters) throws RESTException {
	  
		RESTResource resource = new RESTResource(name(uri), this);
		resource.contentType = "application/ld+json";
		
		List<String> translations = new ArrayList<String>();
				
		// Return all Translations
		try {
			translations = TranslateUtils.listTranslations("/translate-fail");
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