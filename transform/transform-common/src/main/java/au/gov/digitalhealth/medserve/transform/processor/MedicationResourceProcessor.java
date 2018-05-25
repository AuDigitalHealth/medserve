package au.gov.digitalhealth.medserve.transform.processor;

import java.io.IOException;
import java.util.List;

import org.hl7.fhir.dstu3.model.Resource;

public interface MedicationResourceProcessor {
    void processResources(List<? extends Resource> resources) throws IOException;
}
