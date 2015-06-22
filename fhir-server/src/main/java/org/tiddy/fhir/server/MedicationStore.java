package org.tiddy.fhir.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.Medication;
import ca.uhn.fhir.model.dstu2.resource.Substance;
import ca.uhn.fhir.parser.IParser;

@ApplicationScoped
public class MedicationStore {

    CoreContainer container;
    EmbeddedSolrServer server;
    IParser parser = FhirContext.forDstu2().newJsonParser();

    private static Logger logger = Logger.getLogger(MedicationStore.class
            .getSimpleName());

    @PostConstruct
    public void postConstruct() {
        try {
            System.setProperty("solr.solr.home",
                    "/opt/hl7/medications.store/solr");
            CoreContainer.Initializer initializer = new CoreContainer.Initializer();
            container = initializer.initialize();

            server = new EmbeddedSolrServer(container, "collection1");
            server.deleteByQuery("*:*");
            addMedication(FileUtils.readFileToString(new File(
                    "/opt/hl7/medications.store/MEDICATION.json")));

            addSubstance(FileUtils.readFileToString(new File(
                    "/opt/hl7/medications.store/SUBSTANCE.json")));

        } catch (Exception e) {
            throw new RuntimeException("MedicationStore::postConstruct", e);
        }
    }

    public List<String> searchSubstanceById(String id) {
        try {
            SolrQuery query = new SolrQuery();
            query.setQuery("id:" + id + " AND subject:substance");
            QueryResponse rsp = server.query(query);
            SolrDocumentList docs = rsp.getResults();
            List<String> list = new ArrayList<>();
            docs.forEach(new Consumer<SolrDocument>() {
                @Override
                public void accept(SolrDocument t) {
                    list.add(String.class.cast(t.getFieldValue("author")));
                }
            });
            return list;
        } catch (Exception e) {
            throw new RuntimeException("MedicationStore::searchSubstanceById",
                    e);
        }
    }

    public List<String> searchMedicationById(String id) {
        try {
            SolrQuery query = new SolrQuery();
            query.setQuery("id:" + id + " AND subject:medication");
            QueryResponse rsp = server.query(query);
            SolrDocumentList docs = rsp.getResults();
            List<String> list = new ArrayList<>();
            docs.forEach(new Consumer<SolrDocument>() {
                @Override
                public void accept(SolrDocument t) {
                    list.add(String.class.cast(t.getFieldValue("author")));
                }
            });
            return list;
        } catch (Exception e) {
            throw new RuntimeException("MedicationStore::searchMedicationById",
                    e);
        }
    }

    public void addMedication(String m) {
        try {
            Medication medication = parser.parseResource(Medication.class, m);
            SolrInputDocument document = new SolrInputDocument();
            document.addField("id", medication.getId().getIdPartAsLong().toString());
            document.addField("name", medication.getName());
            document.addField("subject", "medication");
            document.addField("author", m);
            server.add(document);

            server.commit();
        } catch (Exception e) {
            throw new RuntimeException("MedicationStore::addMedication", e);
        }
    }

    public void addSubstance(String s) {
        try {
            Substance substance = parser.parseResource(Substance.class, s);
            SolrInputDocument document = new SolrInputDocument();
            document.addField("id", substance.getId().getIdPartAsLong().toString());
            document.addField("name", substance.getText());
            document.addField("subject", "substance");
            document.addField("author", s);
            server.add(document);

            server.commit();
        } catch (Exception e) {
            throw new RuntimeException("MedicationStore::addSubstance", e);
        }
    }

    public List<String> searchMedicationByName(String name) {
        try {
            SolrQuery query = new SolrQuery();
            query.setQuery("name:*" + name + "*");
            QueryResponse rsp = server.query(query);
            SolrDocumentList docs = rsp.getResults();
            List<String> list = new ArrayList<>();
            docs.forEach(new Consumer<SolrDocument>() {
                @Override
                public void accept(SolrDocument t) {
                    list.add(String.class.cast(t.getFieldValue("author")));
                }
            });
            return list;
        } catch (Exception e) {
            throw new RuntimeException("MedicationStore::searchMedicationById",
                    e);
        }
    }

}
