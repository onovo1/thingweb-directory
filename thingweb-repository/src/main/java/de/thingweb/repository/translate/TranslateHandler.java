package de.thingweb.repository.translate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import de.thingweb.repository.Repository;
import de.thingweb.repository.ThingDescriptionUtils;
import de.thingweb.repository.rest.BadRequestException;
import de.thingweb.repository.rest.RESTException;
import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTResource;
import de.thingweb.repository.rest.RESTServerInstance;

public class TranslateHandler extends RESTHandler {
	
	public TranslateHandler(String id, List<RESTServerInstance> instances) {
		super(id, instances);
	}
	
	@Override
	public RESTResource get(URI uri, Map<String, String> parameters) throws RESTException {
		RESTResource resource = new RESTResource(uri.toString(),this);
		
		resource.contentType = "application/ld+json";
		
		Dataset dataset = Repository.get().dataset;
		dataset.begin(ReadWrite.READ);
		
		try {
			String q = "SELECT ?str WHERE { <" + uri + "> <" + DC.source + "> ?str }";
			QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
			ResultSet result = qexec.execSelect();
			
			if (result.hasNext()) {		
				resource.content = result.next().get("str").asLiteral().getLexicalForm();
			} else {
				throw new RESTException();
			}
		} finally {
			dataset.end();
		}
		
		return resource;
	}
	   
	@Override
	public void put(URI uri, Map<String, String> parameters, InputStream payload) throws RESTException {
		
		String data = "";
		try {
			data = ThingDescriptionUtils.streamToString(payload);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new BadRequestException();
		}
		
		// Check if the translation already exist in the dataset
		if (TranslateUtils.getTranslateFromURI(uri.toString())== null) {
			throw new BadRequestException();
		}
		
		Dataset dataset = Repository.get().dataset;
		dataset.begin(ReadWrite.WRITE);
		
		try {
			Model tdb = dataset.getDefaultModel();
			String modified;
				
			// Save properties of translation's modified
			ThingDescriptionUtils utils = new ThingDescriptionUtils();
			modified = utils.getCurrentDateTime(0);
				
			// Remove properties and add new content
			tdb.getResource(uri.toString()).removeProperties().addProperty(DC.source, data);
				
			// Store properties.
			tdb.getResource(uri.toString()).addProperty(DCTerms.modified, modified);
			
			dataset.commit();
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RESTException();
		} finally {
			dataset.end();
		}
	}
	
	@Override
	public void delete(URI uri, Map<String, String> parameters, InputStream payload) throws RESTException {
		Dataset dataset = Repository.get().dataset;
		dataset.begin(ReadWrite.WRITE);
		try {
			dataset.getDefaultModel().createResource(uri.toString()).removeProperties();
			deleteToAll(uri.getPath());
			dataset.commit();
									
		} catch (Exception e) {
			throw new RESTException();
		} finally {
			dataset.end();
		}
	}

}