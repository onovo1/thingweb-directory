package de.thingweb.directory.translate;

import java.io.IOException;
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
import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTHandler;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.rest.RESTServerInstance;

public class TranslateCollectionHandler extends RESTHandler {

	public TranslateCollectionHandler(List<RESTServerInstance> instances) {
		super("translate", instances);
	}

	@Override
	public RESTResource post(URI uri, Map<String, String> parameters, InputStream payload) throws RESTException {
		
		RESTResource resource = new RESTResource(name(uri), this);
		String data = "";
		String source = "";
		String target = "";
		String rt	  = "";
		
		try {
			data = ThingDescriptionUtils.streamToString(payload);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new BadRequestException(null);
		}
		
		// GET source and target TD IDs from query parameters
		if (parameters.containsKey("source") && !parameters.get("source").isEmpty() && parameters.containsKey("target")
				&& !parameters.get("target").isEmpty() && parameters.containsKey("rt") && !parameters.get("rt").isEmpty()) {
			source = TranslateUtils.getid(parameters.get("source"));
			target = TranslateUtils.getid(parameters.get("target"));
			rt	   = parameters.get("rt");
		} else {
			throw new BadRequestException(null);
		}
		
		String id = source + "_" + target + "_" + rt;
		
		// Checking if the translation has already been registered in the dataset.
		String registeredTranslation = TranslateUtils.getTranslateFromId(uri, id);
		
		if (!registeredTranslation.isEmpty()){
			throw new BadRequestException(null);
		}
		
		// to add new translation to the collection
		URI resourceUri = URI.create(normalize(uri) + "/" + id);
		
		Dataset dataset = ThingDirectory.get().dataset;

		dataset.begin(ReadWrite.WRITE);
		try {
			
			Model tdb = dataset.getDefaultModel();
			tdb.createResource(resourceUri.toString()).addProperty(DC.source, data);
			ThingDescriptionUtils utils = new ThingDescriptionUtils();
			
			String currentDate = utils.getCurrentDateTime(0);
			tdb.getResource(resourceUri.toString()).addProperty(DCTerms.created, currentDate);
			tdb.getResource(resourceUri.toString()).addProperty(DCTerms.modified, currentDate);
			
			addToAll("/translate/" + id, new TranslateHandler(id, instances));
			dataset.commit();
			
			
			resource = new RESTResource("/translate/" + id,
					new TranslateHandler(id, instances));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RESTException();
		} finally {
			dataset.end();
		}
		
		//Create an instance of the class
		TranslateFailHandler translateFailHandler = new TranslateFailHandler(registeredTranslation, instances);
		
		//Delete the fail translation if it exist in the database
		translateFailHandler.delete(uri, id);
		
		return resource;
	}
	
	private String normalize(URI uri) {
		if (!uri.getScheme().equals("http")) {
			return uri.toString().replace(uri.getScheme(), "http");
		}
		return uri.toString();
	}
	
	private String name(URI uri) {
		String path = uri.getPath();
		if (path.contains("/")) {
			return path.substring(uri.getPath().lastIndexOf("/") + 1);
		}
		return path;
	}

}
