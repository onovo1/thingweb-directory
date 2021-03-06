package de.thingweb.directory.handlers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base32;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.ThingDescription;
import de.thingweb.directory.ThingDescriptionUtils;
import de.thingweb.directory.VocabularyUtils;
import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.NotFoundException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTHandler;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.rest.RESTServerInstance;

public class ThingDescriptionCollectionHandler extends RESTHandler {

	// for Resource Directory
	public static final String LIFE_TIME = "lt";
	public static final String END_POINT = "ep";
	
	public ThingDescriptionCollectionHandler(List<RESTServerInstance> instances) {
		super("td", instances);
	}
	
	@Override
	public RESTResource get(URI uri, Map<String, String> parameters) throws RESTException {
	  
		RESTResource resource = new RESTResource(name(uri), this);
		resource.contentType = "application/ld+json";
		resource.content = "{";
		
		List<String> tds = new ArrayList<String>();
		String query;
		
		// Normal SPARQL query
		if (parameters.containsKey("query") && !parameters.get("query").isEmpty()) {
			
			query = parameters.get("query");
			try {
				tds = ThingDescriptionUtils.listThingDescriptions(query);
			} catch (Exception e) {
				throw new BadRequestException(null);
			}
			
		} else if (parameters.containsKey("text") && !parameters.get("text").isEmpty()) { // Full text search query
			
			query = parameters.get("text");
			try {
				tds = ThingDescriptionUtils.listThingDescriptionsFromTextSearch(query);
			} catch (Exception e) {
				throw new BadRequestException(null);
			}
			
		} else if (parameters.containsKey("rdf") && !parameters.get("rdf").isEmpty()) { // RDF type/value type query
			
			query = parameters.get("rdf");
			try {
				tds = ThingDescriptionUtils.listRDFTypeValues(query);
			} catch (Exception e) {
				e.printStackTrace();
				throw new BadRequestException(null);
			}
			
			// Retrieve type values
			for (int i = 0; i < tds.size(); i++) {
				resource.content += "\"unit\": " + tds.get(i);
				if (i < tds.size() - 1) {
					resource.content += ",\n";
				}
			}
			
			resource.content += "}";
			return resource;
			
		} else {
			// Return all TDs
			try {
				tds = ThingDescriptionUtils.listThingDescriptions("?s ?p ?o");
			} catch (Exception e) {
				throw new BadRequestException(null);
			}
		}
		
		// Retrieve Thing Description(s)
		for (int i = 0; i < tds.size(); i++) {
			URI td = URI.create(tds.get(i));
			
			try {
				ThingDescriptionHandler h = new ThingDescriptionHandler(td.toString(), instances);
				RESTResource res = h.get(td, new HashMap<String, String>());
				// TODO check TD's content type
				
				resource.content += "\"" + td.getPath() + "\": " + res.content;
				if (i < tds.size() - 1) {
					resource.content += ",";
				}
				
			}  catch (NotFoundException e) {
				// remove ","
				if (resource.content.endsWith(",")) {
					resource.content = resource.content.substring(0, resource.content.length() -1);
				}
				continue; // Life time is invalid and TD was removed
				
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Unable to retrieve Thing Description " + td.getPath());
			}
		}
		
		resource.content += "}";
		return resource;
	}

	@Override
	public RESTResource post(URI uri, Map<String, String> parameters, InputStream payload) throws RESTException {		

		
		boolean registeredTD = false;
		String data = "";
		String id = "";
		try {
			data = ThingDescriptionUtils.streamToString(payload);
			data = ThingDescriptionUtils.withLocalJsonLdContext(data);
			//Generate ID of the data
			id = generateID(data);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new BadRequestException(null);
		}
				
		// Check if new TD has uris already registered in the dataset or that TD is already registered
		try {			
			registeredTD = ThingDescriptionUtils.registeredTD(data, uri.toString()+ "/" + id);
		} catch (URISyntaxException e) {
			throw new RESTException();
		}

		if (registeredTD){
			throw new BadRequestException(uri.getPath()+ "/" + id);
		}

		// Check if new TD has uris already registered in the dataset
		/*if (ThingDescriptionUtils.hasInvalidURI(data)) {
			throw new BadRequestException();
		}*/
		
		// to register a resource following the standard
		String endpointName = "http://example.org/"; // this is temporary
		String lifeTime = "86400"; // 24 hours by default in seconds
		
		// TODO make it mandatory. The rest are optional
		if (parameters.containsKey(END_POINT) && !parameters.get(END_POINT).isEmpty()) {
			endpointName = parameters.get(END_POINT);	
		}
		
		if (parameters.containsKey(LIFE_TIME) && !parameters.get(LIFE_TIME).isEmpty()) {
			lifeTime = parameters.get(LIFE_TIME);
			// TODO enforce a minimal lifetime
		}
		
		// to add new thing description to the collection
		//String id = generateID(data);
		URI resourceUri = URI.create(normalize(uri) + "/" + id);
		Dataset dataset = ThingDirectory.get().dataset;
		List<String> keyWords;
		Model schema = VocabularyUtils.mergeVocabularies();

		dataset.begin(ReadWrite.WRITE);
		try {
			
			//New graph model
			Model graph = ModelFactory.createDefaultModel();
			//graph.read(new ByteArrayInputStream(data.getBytes()), endpointName, "JSON-LD");
			graph.read(new ByteArrayInputStream(data.getBytes()), null, "JSON-LD");
			InfModel inf = ModelFactory.createInfModel(ReasonerRegistry.getOWLMicroReasoner(), schema, graph);
			

			ThingDescriptionUtils utils = new ThingDescriptionUtils();
			keyWords = utils.getModelKeyWords(graph);
			// TODO check TD validity
			
			dataset.addNamedModel(resourceUri.toString(), inf.difference(schema));
		/*
		dataset.begin(ReadWrite.WRITE);
		try {
		
			//New graph model
			Model graph = dataset.getNamedModel(resourceUri.toString());
			graph.read(new ByteArrayInputStream(data.getBytes()), null, "JSON-LD");
			ThingDescriptionUtils utils = new ThingDescriptionUtils();
			keyWords = utils.getModelKeyWords(graph);
			// TODO check TD validity
			//dataset.close();
		*/
			//Adding the information of the new TD into the Default Model
			Model tdb = dataset.getDefaultModel();
			tdb.createResource(resourceUri.toString()).addProperty(DC.source, data);
		
			// Store key words as triple: ?g_id rdfs:comment "keyWordOrWords"
			tdb.getResource(resourceUri.toString()).addProperty(RDFS.comment, StrUtils.strjoin(" ", keyWords));
		
			// Store END_POINT and LIFE_TIME as triples
			String currentDate = utils.getCurrentDateTime(0);
			String lifetimeDate = utils.getCurrentDateTime(Integer.parseInt(lifeTime));
			tdb.getResource(resourceUri.toString()).addProperty(RDFS.isDefinedBy, endpointName);
			tdb.getResource(resourceUri.toString()).addProperty(DCTerms.created, currentDate);
			tdb.getResource(resourceUri.toString()).addProperty(DCTerms.modified, currentDate);
			tdb.getResource(resourceUri.toString()).addProperty(DCTerms.dateAccepted, lifetimeDate);
		
			addToAll("/td/" + id, new ThingDescriptionHandler(id, instances));
			dataset.commit();

			ThingDirectory.LOG.info(String.format("Inserted TD %s (%d triples)", id, graph.size()));
			
			// Add to priority queue
			ThingDescription td = new ThingDescription(resourceUri.toString(), lifetimeDate);
			ThingDirectory.get().tdQueue.add(td);
			ThingDirectory.get().setTimer();
					
			// TODO remove useless return
			RESTResource resource = new RESTResource("/td/" + id, new ThingDescriptionHandler(id, instances));
			return resource;

		} catch (Exception e) {
			e.printStackTrace();
			throw new RESTException();
		} finally {
			dataset.end();
		}
	}
	
	
	@Override
	public RESTResource observe(URI uri, Map<String, String> parameters) throws RESTException {
		
		return get(uri, null);
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
	
	public static String generateID(String name) {
		
		String id = "";
		Base32 base32 = new Base32();
		ByteBuffer b = ByteBuffer.allocate(4);
		
		//get the hash code from the TD
		int hash = name.hashCode();
		
		//convert int into array
		byte[] hash_array = b.putInt(hash).array();
		id = base32.encodeAsString(hash_array);
		
		//trim the string to a max of 10 characters
		id = id.substring(0, Math.min(id.length(), 10));
		
		//remove # in the string if exist
		id = id.replaceAll("=", "");
		
		//convert the string to lower case
		return id.toLowerCase();
		
		// TODO better way?
		//String id = UUID.fromString(name).toString();	
		//return id.substring(0, id.indexOf('-'));
	}

}
