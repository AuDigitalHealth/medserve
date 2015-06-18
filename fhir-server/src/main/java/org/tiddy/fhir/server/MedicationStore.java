package org.tiddy.fhir.server;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;

@ApplicationScoped
public class MedicationStore {

	CoreContainer container;
	EmbeddedSolrServer server;

	@PostConstruct
	public void postConstruct() {
		try {
			String solrDir = "/opt/hl7/medications.store/solr";
			container = new CoreContainer(solrDir);
			container.load();
			server = new EmbeddedSolrServer(container, "medications.store");
			// Collection<File> files = FileUtils.listFiles(new File(
			// "sandbox/sonnets"), TrueFileFilter.INSTANCE,
			// TrueFileFilter.INSTANCE);
			// for (File file : files) {
			// String name = file.getName();
			// String content = FileUtils.readFileToString(file);
			// SolrInputDocument document = new SolrInputDocument();
			// document.addField("name", name);
			// document.addField("text", content);
			// document.addField("id", name.hashCode());
			// server.add(document);
			// }

			server.commit();
		} catch (Exception e) {
			throw new RuntimeException("MedicationStore::postConstruct", e);
		}
	}
}
