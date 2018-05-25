package au.gov.digitalhealth.medserve.transform.amt.enumeration;

import java.util.HashMap;

public enum AttributeType {
    // @formatter:off
    IS_A(116680003), 
    HAS_UNIT(177631000036102L), 
    HAS_MPUU(30348011000036104L), 
    HAS_AUSTRALIAN_BOSS(30364011000036101L), 
    IS_MODIFICATION_OF(30394011000036104L), 
    HAS_TPUU(30409011000036107L), 
    HAS_SUBPACK(30454011000036104L), 
    HAS_CONTAINER_TYPE(30465011000036106L), 
    HAS_TPP(30488011000036103L), 
    HAS_MANUFACTURED_DOSE_FORM(30523011000036108L), 
    HAS_UNIT_OF_USE(30548011000036101L), 
    HAS_COMPONENT_PACK(700000061000036106L), 
    HAS_DENOMINATOR_UNITS(700000071000036103L), 
    HAS_INTENDED_ACTIVE_INGREDIENT(700000081000036101L), 
    HAS_NUMERATOR_UNITS(700000091000036104L), 
    HAS_TP(700000101000036108L),
    STRENGTH(700000111000036105L), 
    UNIT_OF_USE_SIZE(700000141000036106L), 
    UNIT_OF_USE_QUANTITY(700000131000036101L),
    SUBPACK_QUANTITY(700000121000036103L);
    // @formatter:on

    private static HashMap<Long, AttributeType> instanceMap = new HashMap<>();

    static {
        for (AttributeType instance : AttributeType.values()) {
            instanceMap.put(instance.id, instance);
        }
    }

    public static AttributeType fromId(long id) {
        if (instanceMap.containsKey(id)) {
            return instanceMap.get(id);
        } else {
            throw new RuntimeException("Cannot find enum for id " + id);
        }
    }

    private long id;

    private AttributeType(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public String getIdString() {
        return Long.toString(id);
    }

    public static AttributeType fromIdString(String idString) {
        return fromId(Long.parseLong(idString));
    }

    public static boolean isEnumValue(String idString) {
        return instanceMap.containsKey(Long.parseLong(idString));
    }

}
