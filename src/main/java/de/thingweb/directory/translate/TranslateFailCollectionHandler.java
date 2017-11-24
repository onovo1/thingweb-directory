package de.thingweb.directory.translate;

import java.util.List;
import de.thingweb.directory.rest.RESTHandler;
import de.thingweb.directory.rest.RESTServerInstance;

public class TranslateFailCollectionHandler extends RESTHandler {

	public TranslateFailCollectionHandler(List<RESTServerInstance> instances) {
		super("translate-fail", instances);
	}
}
