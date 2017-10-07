package de.thingweb.directory.translate;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.ThingDescriptionUtils;
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
	
	public static void addEntry(URI uri, String id) {
		
		// Check if the parameter is null
		if (id.isEmpty()) {
			return;
		} 
		
		// create the new id for the entry
		String str_uri = "http://" + uri.getHost() + "/translate-fail/" + id;
		
		// Checking if the translation has already registered in the dataset.
		String registeredFailTranslation = TranslateUtils.getFailTranslateFromId(str_uri);
		
		if (!registeredFailTranslation.isEmpty()){
			return;
		}
		
		Dataset dataset = ThingDirectory.get().dataset;

		dataset.begin(ReadWrite.WRITE);
		try {
			
			Model tdb = dataset.getDefaultModel();
			ThingDescriptionUtils utils = new ThingDescriptionUtils();
			
			String currentDate = utils.getCurrentDateTime(0);
			tdb.createResource(str_uri).addProperty(DC.source, "");
			tdb.getResource(str_uri).addProperty(DCTerms.created, currentDate);
			tdb.getResource(str_uri).addProperty(DCTerms.modified, currentDate);
			
			dataset.commit();
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			dataset.end();
		}	
	}
	
	
	public static void delete(URI uri, String id) {

		// Check if the parameter is null
		if (id.isEmpty()) {
			return;
		} 
		
		// create the id of the entry
		String str_uri = "http://" + uri.getHost() + "/translate-fail/" + id;
		
		// Checking if the translation has already registered in the dataset.
		String registeredFailTranslation = TranslateUtils.getFailTranslateFromId(str_uri);
		if (registeredFailTranslation.isEmpty()){
			return;
		}
		
		Dataset dataset = ThingDirectory.get().dataset;
		dataset.begin(ReadWrite.WRITE);
		try {
			//dataset.getDefaultModel().getResource(resourceUri).removeProperties();
			dataset.getDefaultModel().createResource(str_uri).removeProperties();
			dataset.commit();
									
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			dataset.end();
		}
	}
	
	private String name(URI uri) {
		
		String path = uri.getPath();
		if (path.contains("/")) {
			return path.substring(uri.getPath().lastIndexOf("/") + 1);
		}
		return path;
	}
}
