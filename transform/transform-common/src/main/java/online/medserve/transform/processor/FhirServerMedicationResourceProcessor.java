package online.medserve.transform.processor;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.Bundle.HTTPVerb;
import org.hl7.fhir.dstu3.model.Resource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.IGenericClient;
import online.medserve.transform.util.LoggingTimer;

public class FhirServerMedicationResourceProcessor implements MedicationResourceProcessor {

    private static final Logger logger =
            Logger.getLogger(FhirServerMedicationResourceProcessor.class.getCanonicalName());
    private IGenericClient client;

    public FhirServerMedicationResourceProcessor(String url, int timeout) throws IOException {
        FhirContext ctx = FhirContext.forDstu3();
        ctx.getRestfulClientFactory().setConnectionRequestTimeout(timeout);
        ctx.getRestfulClientFactory().setSocketTimeout(timeout);
        client = ctx.newRestfulGenericClient(url);
        logger.info("FHIR client created to " + url + " with timeout " + timeout);
    }

    @Override
    public void processResources(List<? extends Resource> resources) throws IOException {
        try (
                LoggingTimer timer = new LoggingTimer(logger,
                    "Submitting " + resources.size() + " resources to FHIR server as a transaction")) {
            for (Resource resource : resources) {
                Bundle bundle = new Bundle();
                bundle.setType(BundleType.TRANSACTION);
                bundle.addEntry()
                    .setFullUrl(resource.getId())
                    .setResource(resource)
                    .getRequest()
                    .setUrl(resource.getResourceType().name() + "/" + resource.getId())
                    .setMethod(HTTPVerb.PUT);
                client.transaction().withBundle(bundle).execute();
            }
        }
    }
}
