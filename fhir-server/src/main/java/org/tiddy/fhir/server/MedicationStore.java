package org.tiddy.fhir.server;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;

import ca.uhn.fhir.model.dstu2.resource.Medication;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@ApplicationScoped
public class MedicationStore {

	CoreContainer container;
	EmbeddedSolrServer server;

	private static Logger logger = Logger.getLogger(MedicationStore.class
			.getSimpleName());
	private static Gson gson = new GsonBuilder().create();

	@PostConstruct
	public void postConstruct() {
		try {
			System.setProperty("solr.solr.home",
					"/opt/hl7/medications.store/solr");
			CoreContainer.Initializer initializer = new CoreContainer.Initializer();
			container = initializer.initialize();

			server = new EmbeddedSolrServer(container, "collection1");

		} catch (Exception e) {
			throw new RuntimeException("MedicationStore::postConstruct", e);
		}
	}

	public void search() {
		try {
			SolrQuery query = new SolrQuery();
			query.setQuery("*:*");
			QueryResponse rsp = server.query(query);
			SolrDocumentList docs = rsp.getResults();
		} catch (Exception e) {
			throw new RuntimeException("MedicationStore::search", e);
		}
	}
	
	public void add(Medication m) {
		try {
			SolrInputDocument document = new SolrInputDocument();
			document.addField("id", m.getId());
			document.addField("text", gson.toJson(m));
			document.addField("id", "name".hashCode());
			server.add(document);

			server.commit();
		} catch (Exception e) {
			throw new RuntimeException("MedicationStore::postConstruct", e);
		}		
	}
}
