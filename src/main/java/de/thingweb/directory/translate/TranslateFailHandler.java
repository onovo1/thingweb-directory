package de.thingweb.directory.translate;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.ThingDescriptionUtils;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTHandler;
import de.thingweb.directory.rest.RESTServerInstance;

public class TranslateFailHandler extends RESTHandler {
	
	public TranslateFailHandler(String id, List<RESTServerInstance> instances) {
		super(id, instances);
	}
	
	public void addEntry(URI uri, String id) {
		
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
			addToAll("/translate-fail/" + id, new TranslateHandler(id, instances));
			dataset.commit();
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			dataset.end();
		}	
	}
	
	@Override
	public void delete(URI uri, Map<String, String> parameters, InputStream payload) throws RESTException {
		Dataset dataset = ThingDirectory.get().dataset;
		dataset.begin(ReadWrite.WRITE);
		try {
			dataset.getDefaultModel().createResource(uri.toString()).removeProperties();
			dataset.removeNamedModel(uri.toString());		
			deleteToAll(uri.getPath());
			dataset.commit();
			
		} catch (Exception e) {
			throw new RESTException();
		} finally {
			dataset.end();
		}
	}
	
	public void delete(URI uri, String id) throws RESTException {

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
			dataset.removeNamedModel(str_uri);
            deleteToAll("/translate-fail/" + id);
			dataset.commit();
									
		} catch (Exception e) {
			// TODO distinguish between client and server errors
			throw new RESTException();
		} finally {
			dataset.end();
		}
	}

}
