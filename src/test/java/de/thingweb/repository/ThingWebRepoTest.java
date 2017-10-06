package de.thingweb.repository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonString;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.ThingDescriptionUtils;
import de.thingweb.directory.VocabularyUtils;
import de.thingweb.directory.coap.CoAPServer;
import de.thingweb.directory.handlers.TDLookUpEPHandler;
import de.thingweb.directory.handlers.TDLookUpHandler;
import de.thingweb.directory.handlers.TDLookUpSEMHandler;
import de.thingweb.directory.handlers.ThingDescriptionCollectionHandler;
import de.thingweb.directory.handlers.ThingDescriptionHandler;
import de.thingweb.directory.handlers.VocabularyCollectionHandler;
import de.thingweb.directory.handlers.VocabularyHandler;
import de.thingweb.directory.handlers.WelcomePageHandler;
import de.thingweb.directory.http.HTTPServer;
import de.thingweb.directory.rest.RESTHandler;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.rest.RESTServerInstance;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;

public class ThingWebRepoTest {

	private static ThingDescriptionCollectionHandler tdch;
	private static VocabularyCollectionHandler vch;
	
	private final static int portCoap = 5683;
	private final static int portHttp = 8080;
	private final static String dbPath  = "DB-test";
	private final static String idxPath = "Lucene-test";
	private static String baseUri = "http://www.example.com";

	@BeforeClass
	public static void oneTimeSetUp() {
		
		// Setup repository
		ThingDirectory.get().init(dbPath, baseUri, idxPath);
		
		List<RESTServerInstance> servers = new ArrayList<>();
		RESTHandler root = new WelcomePageHandler(servers);
		servers.add(new CoAPServer(portCoap, root));
        servers.add(new HTTPServer(portHttp, root));

        for (RESTServerInstance i : servers) {
            i.add("/td-lookup", new TDLookUpHandler(servers));
            i.add("/td-lookup/ep", new TDLookUpEPHandler(servers));
            i.add("/td-lookup/sem", new TDLookUpSEMHandler(servers));
            i.add("/td", new ThingDescriptionCollectionHandler(servers));
            i.add("/vocab", new VocabularyCollectionHandler(servers));
            i.start();
        }
        
        ThingDirectory.get();
		ThingDirectory.servers = servers;
		
		tdch = new ThingDescriptionCollectionHandler(servers);
		vch = new VocabularyCollectionHandler(servers);

	}

	@AfterClass
	public static void oneTimeTearDown() {
		// Close dataset
		Dataset ds = ThingDirectory.get().dataset;
		ds.close();
		
		deleteAll(dbPath); // FIXME returns false?
		deleteAll(idxPath);
	}

	
	@Test
	public void testTDManagement() throws IOException, URISyntaxException {
		
		RESTResource resource;
		byte[] content;
		String tdId, tdId2, td;
		ThingDescriptionHandler tdh;
		
		// LOOKUP
		Set<String> tdIds;
		JsonObject fanQR;

		Map<String,String> parameters = new HashMap<String,String>();
		
		//DELETE EXISTING TDs FIRST

		parameters.clear();
		resource = tdch.get(new URI(baseUri + "/td"), parameters);

		fanQR = JSON.parse(resource.content);
		tdIds = fanQR.keys();
		
		tdId = resource.path;
		tdh = new ThingDescriptionHandler(tdId, ThingDirectory.get().servers);
		
		for (String item : tdIds){
			tdh.delete(new URI(baseUri + item), null, null);
		}

		parameters.clear();
		parameters.put("ep", baseUri);
		
		// POST TD fan
		String tdUri = "coap:///www.example.com:5686/Fan";
		InputStream in = ThingDirectory.get().getClass().getClassLoader().getResourceAsStream("samples/fanTD.jsonld");
		resource = tdch.post(new URI(baseUri + "/td"), parameters, in);
		tdId = resource.path;
		
		td = ThingDescriptionUtils.getThingDescriptionIdFromUri(tdUri);
		Assert.assertEquals("TD fan not registered", baseUri + tdId, td);
		
		
		// POST TD temperatureSensor
		String tdUri2 = "coap:///www.example.com:5687/temp";
		in = ThingDirectory.get().getClass().getClassLoader().getResourceAsStream("samples/temperatureSensorTD.jsonld");
		resource = tdch.post(new URI(baseUri + "/td"), parameters, in);
		tdId2 = resource.path;
		
		td = ThingDescriptionUtils.getThingDescriptionIdFromUri(tdUri2);
		Assert.assertEquals("TD temperatureSensor not registered", baseUri + tdId2, td);
		in.close();
				
		// GET by sparql query
		parameters.clear();
		parameters.put("query", "?s ?p ?o");
		resource = tdch.get(new URI(baseUri + "/td"), parameters);
		
		fanQR = JSON.parse(resource.content);
		tdIds = fanQR.keys();
		Assert.assertFalse("TD is Empty", tdIds.isEmpty());
		//Assert.assertEquals("Found more than one TD", 1, tdIds.size());
		Assert.assertTrue("TD fan not found", tdIds.contains(tdId));
		
		
		// GET by text query
		parameters.clear();
		parameters.put("text", "\"name AND fan\"");
		resource = tdch.get(new URI(baseUri + "/td"), parameters);
		
		fanQR = JSON.parse(resource.content);
		tdIds = fanQR.keys();
		Assert.assertFalse("TD fan not found", tdIds.isEmpty());
		Assert.assertTrue("TD fan not found", tdIds.contains(tdId));
		Assert.assertFalse("TD temperatureSensor found", tdIds.contains(tdId2));
		
		// GET TD by id
		tdh = new ThingDescriptionHandler(tdId, ThingDirectory.get().servers);
		resource = tdh.get(new URI(baseUri + tdId), null);
		JsonObject o = JSON.parse(resource.content);
		JsonValue v = o.get("base");
		Assert.assertEquals("TD fan not found", "\"" + tdUri + "\"", v.toString());
		
		
		// PUT TD change fan's name
		in = ThingDirectory.get().getClass().getClassLoader().getResourceAsStream("samples/fanTD_update.jsonld");
		content = IOUtils.toByteArray(in);
		tdh.put(new URI(baseUri + tdId), new HashMap<String,String>(), new ByteArrayInputStream(content));
			
		// GET TD by id and check change
		RESTResource resource2 = tdh.get(new URI(baseUri + tdId), null);
		JsonObject o2 = JSON.parse(resource2.content);
		JsonValue v2 = o2.get("name");
		Assert.assertEquals("TD fan not updated", "\"Fan2\"", v2.toString());

		
		// DELETE TDs
		tdh.delete(new URI(baseUri + tdId), null, null);
		td = ThingDescriptionUtils.getThingDescriptionIdFromUri(tdUri);
		Assert.assertEquals("TD fan not deleted", "NOT FOUND", td);
		
		tdh.delete(new URI(baseUri + tdId2), null, null);
		td = ThingDescriptionUtils.getThingDescriptionIdFromUri(tdUri2);
		Assert.assertEquals("TD temperatureSensor not deleted", "NOT FOUND", td);
		
	}
	
	@Test
	public void testVocabularyManagement() throws Exception {
		RESTResource resource;
		String ontoId;

		Map<String,String> parameters = new HashMap<String,String>();
		
		// POST vocabulary
		String ontoUri = "http://purl.oclc.org/NET/ssnx/qu/qu-rec20";
		InputStream uriStream = new ByteArrayInputStream(ontoUri.getBytes("UTF-8"));
		resource = vch.post(new URI(baseUri + "/vocab"), parameters, uriStream);
		ontoId = resource.path;

		Assert.assertTrue("QU ontology not registered", VocabularyUtils.containsVocabulary(ontoUri));
		
		// GET vocabulary by SPARQL query
		parameters.clear();
		parameters.put("query", "?s ?p ?o");
		resource = vch.get(new URI(baseUri + "/vocab"), parameters);
		
		JsonValue ontoIds = JSON.parseAny(resource.content);
		Assert.assertTrue("Vocabulary collection is not an array", ontoIds.isArray());
		Assert.assertTrue("Vocabulary imports were not added", ontoIds.getAsArray().size() > 1);
		Assert.assertTrue("QU ontology not found", ontoIds.getAsArray().contains(new JsonString(ontoId)));
		
		// GET vocabulary by id
		VocabularyHandler vh = new VocabularyHandler(ontoId, ThingDirectory.servers);
		resource = vh.get(new URI(baseUri + ontoId), null);
		
		ByteArrayInputStream byteStream = new ByteArrayInputStream(resource.content.getBytes());
		Model m = ModelFactory.createDefaultModel();
		m.read(byteStream, "", "Turtle");
		Assert.assertFalse("QU ontology definition is not valid", m.isEmpty());

		// DELETE vocabulary
		vh.delete(new URI(baseUri + ontoId), null, null);
		Assert.assertFalse("QU ontology not deleted", VocabularyUtils.containsVocabulary(ontoUri));
	}
	
	
	
	// ***** EXTRAS *****
	
	
	/**
	 * Returns the content of a TD json-ld file.
	 * Mocks the behavior of doing a GET to the TD's uri.
	 * 
	 * @param filePath Path of the json-ld file.
	 * @return Content of the file in a String.
	 * @throws IOException
	 */
	private byte[] getThingDescription(URI filePath) throws IOException {
		
		return Files.readAllBytes(Paths.get(filePath));
	}
	
	private static boolean deleteAll(String dirPath) {
		File dir = new File(dirPath);
		for (File f : dir.listFiles()) {
			f.delete();
		}
		return dir.delete();
	}

}
