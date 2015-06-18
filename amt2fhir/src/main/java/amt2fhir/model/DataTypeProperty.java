package amt2fhir.model;

import amt2fhir.enumeration.AttributeType;

public class DataTypeProperty {
    private String value;
    private Concept unit;
    private AttributeType type;

    public DataTypeProperty(String value, Concept unit, AttributeType type) {
        super();
        this.value = value;
        this.unit = unit;
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Concept getUnit() {
        return unit;
    }

    public void setUnit(Concept unit) {
        this.unit = unit;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
    }
}
