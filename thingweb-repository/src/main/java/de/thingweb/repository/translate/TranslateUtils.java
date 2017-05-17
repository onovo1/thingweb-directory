package de.thingweb.repository.translate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.DC;

import de.thingweb.repository.Repository;
import de.thingweb.repository.rest.RESTResource;

import org.json.JSONObject;

public class TranslateUtils
{
  /**
   * Returns the translations stored in the database.
   * @return the IDs of all the translations.
   */
  public static List<String> listTranslations(String path) {
	String prefix = StrUtils.strjoinNL("PREFIX  dc: <http://purl.org/dc/elements/1.1/>"
										, "PREFIX  : <.>");
	List<String> tds = new ArrayList<>();
	Dataset dataset = Repository.get().dataset;
	
	dataset.begin(ReadWrite.READ);
	try {
	  //Find all the data of the dataset
	  //System.out.printf("dataset is %s\n", dataset.getDefaultModel().toString());
	    
  	  String select = "SELECT DISTINCT ?s WHERE { ?s dc:source ?o FILTER regex(str(?s), \"" + path + "\", \"i\")}";
	  
	  Query q = QueryFactory.create(prefix + "\n" + select);
	  try (QueryExecution qexec = QueryExecutionFactory.create(q, dataset)) {
		ResultSet result = qexec.execSelect();
		while (result.hasNext()) {
		  tds.add(result.next().get("s").toString());
		}
	  }
	catch (Exception e) {
	  throw e;
	}
	} finally {
	  dataset.end();
	}

	return tds;
  }
  
  public static JSONObject createObject(String source, String target, String rt){
      JSONObject obj = new JSONObject();
      obj.put("source", "/td/" + source);
      obj.put("target", "/td/" + target);
      obj.put("rt", rt);
      return obj;
  }
  
  
  /**
   * Returns the content of a translation stored in the database given its id.
   * @param string URI of the translation we want to return.
   * @return the content of the translation.
   */  
  public static String getTranslateFromURI(String uri) {
	
	String output = "";
	
	Dataset dataset = Repository.get().dataset;
	dataset.begin(ReadWrite.READ);
	try {
		String q = "SELECT ?str WHERE { <" + uri + "> <" + DC.source + "> ?str }";
		
		QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
		ResultSet result = qexec.execSelect();
		if (result.hasNext()) {
			output = result.next().get("str").asLiteral().getLexicalForm();
		}
	} finally {
		dataset.end();
	}
	
	return output;
  }

  /**
   * Returns the last substring of a string limited by '/'.
   * @param string to parse.
   * @return the substring of the URI.
   */
  public static String getid(String uri){
		if (uri.contains("/")) {
			return uri.substring(uri.lastIndexOf("/") + 1);
		}		
      return uri;
  }
  
  
  /**
   * Returns the content of a translation (who has been requested previously but it was not in the database) in the database given its id.
   * @param host's URI of the translation we want to return.
   * @param id String of the translation we want to return.
   * @return the content of the translation.
   */  
  public static String getFailTranslateFromId(String  uri) {
	
	String prefix = StrUtils.strjoinNL("PREFIX  dc: <http://purl.org/dc/elements/1.1/>"
				, "PREFIX  : <.>");

	String output = "";
	
	Dataset dataset = Repository.get().dataset;
	dataset.begin(ReadWrite.READ);
	try {
		String query = "SELECT DISTINCT ?s WHERE {?s dc:source ?source FILTER regex(str(?s), \""+ uri +"\", \"i\")}";
		Query q = QueryFactory.create(prefix + "\n" + query);	
		
		QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
		ResultSet result = qexec.execSelect();
		if (result.hasNext()) {
			output = result.next().get("s").toString();
		}
	} finally {
		dataset.end();
	}
	
	return output;
	
  }
  
  /**
   * Returns the content of a translation stored in the database given its id.
   * @param host's URI of the translation we want to return.
   * @param id String of the translation we want to return.
   * @return the content of the translation.
   */  
  public static String getTranslateFromId(URI uri, String id) {
	
	String query_uri = "http://" + uri.getHost() + "/translate/" + id;
	
	return getTranslateFromURI(query_uri);
	
  }
  
}