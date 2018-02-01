package online.medserve.transform.amt.enumeration;

import java.util.HashMap;

import org.hl7.fhir.dstu3.model.Coding;

import online.medserve.transform.util.FhirCodeSystemUri;

public enum AmtConcept {
    // @formatter:off
    MP(30497011000036103L, "medicinal product"),
    MPUU(30450011000036109L, "medicinal product unit of use"),
    MPP(30513011000036104L, "medicinal product pack"),
    TP(30560011000036108L, "trade product pack"),
    TPUU(30425011000036101L, "trade product unit of use"),
    TPP(30404011000036106L, "trade product pack"),
    CTPP(30537011000036101L, "containered trade product pack"),
    SUBSTANCE(30344011000036106L, "Australian substance"),
    REPLACED_BY(900000000000526001L, "REPLACED BY"),
    POSSIBLY_EQUIVALENT_TO(900000000000523009L, "POSSIBLY EQUIVALENT TO"),
    SAME_AS(900000000000527005L, "SAME AS");
    // @formatter:on

    private static HashMap<Long, AmtConcept> instanceMap = new HashMap<>();

    static {
        for (AmtConcept instance : AmtConcept.values()) {
            instanceMap.put(instance.id, instance);
        }
    }

    public static AmtConcept fromId(long id) {
        if (instanceMap.containsKey(id)) {
            return instanceMap.get(id);
        } else {
            throw new RuntimeException("Cannot find enum for id " + id);
        }
    }

    private long id;
    private String display;

    private AmtConcept(long id, String display) {
        this.id = id;
        this.display = display;
    }

    public long getId() {
        return id;
    }

    public String getIdString() {
        return Long.toString(id);
    }

    public static AmtConcept fromIdString(String idString) {
        return fromId(Long.parseLong(idString));
    }

    public static boolean isEnumValue(String idString) {
        return instanceMap.containsKey(Long.parseLong(idString));
    }

    public static boolean isEnumValue(long id) {
        return instanceMap.containsKey(id);
    }

    public String getDisplay() {
        return display;
    }
    
    public Coding toCoding(){
        return new Coding(FhirCodeSystemUri.SNOMED_CT_SYSTEM_URI.getUri(), getIdString(), display);
    }
}
