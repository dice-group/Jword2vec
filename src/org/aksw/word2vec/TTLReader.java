package org.aksw.word2vec;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.turtle.TurtleParser;

public class TTLReader {

	private HashMap<String, String> map;

	public Map<String, String> loadDBpediaMappings() {
		this.map = new HashMap<String, String>();
		try {
			RDFParser parser = new TurtleParser();
			OnlineStatementHandler osh = new OnlineStatementHandler();
			parser.setRDFHandler(osh);
			parser.setStopAtFirstError(false);
			parser.parse(new FileReader("freebase_links_en.ttl"), "http://dbpedia.org/");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return map;
	}

	private class OnlineStatementHandler extends RDFHandlerBase {
		@Override
		public void handleStatement(Statement st) {
			String subject = st.getSubject().stringValue();
			String object = st.getObject().stringValue();
			map.put(subject, object);
		}
	}

}
