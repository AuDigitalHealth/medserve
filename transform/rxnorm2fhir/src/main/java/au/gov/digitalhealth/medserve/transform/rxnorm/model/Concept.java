package au.gov.digitalhealth.medserve.transform.rxnorm.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;

import au.gov.digitalhealth.medserve.extension.MedicationType;
import au.gov.digitalhealth.medserve.transform.rxnorm.enumeration.RxNormReltaionshipType;
import au.gov.digitalhealth.medserve.transform.rxnorm.enumeration.RxNormType;
import au.gov.digitalhealth.medserve.transform.util.FhirCodeSystemUri;

public class Concept {

    private static final Logger logger = Logger.getLogger(Concept.class.getCanonicalName());

    private long rxcui;
    private String name;
    private RxNormType type;
    private Map<RxNormReltaionshipType, Set<Concept>> relationships = new HashMap<>();
    private Set<Coding> alternativeIds = new HashSet<>();
    CodeableConcept codeableConcept = new CodeableConcept();

    private Set<String> supportedSab = new HashSet<>(Arrays.asList("RXNORM", "ATC", "NDFRT", "NDC", "MTHSPL"));

    public Concept(long rxcui) {
        this.rxcui = rxcui;
    }

    public void addCode(String sab, RxNormType type, String code, String value) {
        if (sab.equals("RXNORM") && type.isBaseType()) {
            setBaseIdentity(type, value);
        } else if (sab.equals("RXNORM") && type.equals(RxNormType.PrescribableName)) {
            setBaseIdentity(type, value);
        } else if (supportedSab.contains(sab)) {
            codeableConcept.addCoding(new Coding(getSystem(sab, type), code, value));
        } else {
            // logger.info("Skipping " + sab + " " + type + " " + code + " " + value);
        }

    }

    private String getSystem(String sab, RxNormType rxType) {
        switch (sab) {
            case "RXNORM":
                return FhirCodeSystemUri.RXNORM_URI.getUri();
            case "ATC":
                return FhirCodeSystemUri.ATC_URI.getUri();
            case "NDFRT":
                return FhirCodeSystemUri.NDFRT_URI.getUri();
            case "NDC":
                return FhirCodeSystemUri.NDC_URI.getUri();
            case "MTHSPL":
                return FhirCodeSystemUri.UNII_URI.getUri();
            default:
                throw new RuntimeException("Cannot get URI for SAB " + sab + " type " + rxType);
        }
    }

    private void setBaseIdentity(RxNormType rxType, String value) {
        if (name != null || type != null) {
            throw new RuntimeException(
                "Trying to set type " + rxType + " and value " + value + " to concept " + this.toString());
        }

        this.type = rxType;
        this.name = value;

        codeableConcept.setText(name);
        codeableConcept.addCoding(new Coding(FhirCodeSystemUri.RXNORM_URI.getUri(), Long.toString(rxcui), name));
    }

    public void addRelationship(RxNormReltaionshipType rela, Concept concept) {
        Set<Concept> targets = relationships.get(rela);
        if (targets == null) {
            targets = new HashSet<>();
            relationships.put(rela, targets);
        }

        targets.add(concept);
    }

    public long getRxcui() {
        return rxcui;
    }

    public String getName() {
        return name;
    }

    public RxNormType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (rxcui ^ (rxcui >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Concept other = (Concept) obj;
        if (rxcui != other.rxcui)
            return false;
        return true;
    }

    public CodeableConcept toCodeableConcept() {
        return codeableConcept;
    }

    public boolean isBaseType() {
        return type.isBaseType();
    }

    public MedicationType getMedicationType() {
        return type.getMedicationType();
    }

    public boolean isActive() {
        // TODO deal with history
        return true;
    }

    public Set<Concept> getTargets(RxNormReltaionshipType relType, RxNormType type) {
        return relationships.get(relType).stream().filter(c -> c.getType().equals(type)).collect(Collectors.toSet());
    }

}
