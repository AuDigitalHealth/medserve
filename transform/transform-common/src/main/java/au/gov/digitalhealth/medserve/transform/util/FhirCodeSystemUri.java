package au.gov.digitalhealth.medserve.transform.util;

import java.util.HashMap;

public enum FhirCodeSystemUri {

    SNOMED_CT_SYSTEM_URI("http://snomed.info/sct"),
    ATC_URI("http://www.whocc.no/atc"),
    TGA_URI("https://www.tga.gov.au/australian-register-therapeutic-goods"),
    PBS_SUBSIDY_URI("http://pbs.gov.au/code/item"),
    PBS_PROGRAM_URI("http://pbs.gov.au/code/program"),
    PBS_RESTRICTION_URI("http://pbs.gov.au/code/restriction"),
    RXNORM_URI("http://www.nlm.nih.gov/research/umls/rxnorm"),
    NDFRT_URI("http://hl7.org/fhir/ndfrt"),
    NDC_URI("http://hl7.org/fhir/sid/ndc"),
    UNII_URI("http://fdasis.nlm.nih.gov");

    private static HashMap<String, FhirCodeSystemUri> instanceMap = new HashMap<>();

    static {
        for (FhirCodeSystemUri instance : FhirCodeSystemUri.values()) {
            instanceMap.put(instance.uri, instance);
        }
    }

    public static FhirCodeSystemUri fromCode(String uri) {
        if (instanceMap.containsKey(uri)) {
            return instanceMap.get(uri);
        } else {
            throw new RuntimeException("Cannot find FhirCodeSystemUri for uri " + uri);
        }
    }

    String uri;

    FhirCodeSystemUri(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

}
