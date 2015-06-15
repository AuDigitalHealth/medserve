package amt2fhir;

import java.util.HashMap;

public enum AmtConcept {
    // @formatter:off
    MP(30497011000036103L),
    MPUU(30450011000036109L),
    MPP(30513011000036104L),
    TP(30560011000036108L),
    TPUU(30425011000036101L),
    TPP(30404011000036106L),
    CTPP(30537011000036101L);
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

    private AmtConcept(long id) {
        this.id = id;
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

}
