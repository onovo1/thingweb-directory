package de.thingweb.directory;

import java.util.HashSet;
import java.util.Set;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;

public class VocabularyUtils {

	public static boolean containsVocabulary(String uri) {
		Dataset dataset = ThingDirectory.get().dataset;
		dataset.begin(ReadWrite.READ);

		try {
			String q = "ASK { GRAPH ?g { <%s> a <http://www.w3.org/2002/07/owl#Ontology> } }";
			QueryExecution qexec = QueryExecutionFactory.create(String.format(q, uri), dataset);
			return qexec.execAsk();
		} catch (Exception e) {
			throw e;
		} finally {
			dataset.end();
		}
	}

	public static Set<String> listVocabularies() {
		Set<String> tds = new HashSet<>();
		Dataset dataset = ThingDirectory.get().dataset;
		dataset.begin(ReadWrite.READ);

		try {
			String q = "SELECT DISTINCT ?g WHERE { GRAPH ?g { ?o a <http://www.w3.org/2002/07/owl#Ontology> } }";
			QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
			ResultSet result = qexec.execSelect();
			while (result.hasNext()) {
				tds.add(result.next().get("g").asResource().getURI());
			}
		} catch (Exception e) {
			throw e;
		} finally {
			dataset.end();
		}

		return tds;
	}

	public static Model mergeVocabularies() {
		// TODO add argument to scope the operation
		Dataset dataset = ThingDirectory.get().dataset;
		dataset.begin(ReadWrite.READ);

		try {
			String q = "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH ?g { ?ontology a <http://www.w3.org/2002/07/owl#Ontology> . ?s ?p ?o } }";
			QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
			return qexec.execConstruct();
		} catch (Exception e) {
			throw e;
		} finally {
			dataset.end();
		}
	}

}