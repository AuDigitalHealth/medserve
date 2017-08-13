package online.medserve.transform.amt.model;

import online.medserve.transform.amt.enumeration.AttributeType;

public class Relationship {
    private Concept source;
    private Concept destination;
    private AttributeType type;
    private DataTypeProperty datatypeProperty;

    public Relationship(Concept source, Concept destination, AttributeType type) {
        this.source = source;
        this.destination = destination;
        this.type = type;
    }

    public Concept getSource() {
        return source;
    }

    public void setSource(Concept source) {
        this.source = source;
    }

    public Concept getDestination() {
        return destination;
    }

    public void setDestination(Concept destination) {
        this.destination = destination;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
    }

    public DataTypeProperty getDatatypeProperty() {
        return datatypeProperty;
    }

    public void setDatatypeProperty(DataTypeProperty datatypeProperty) {
        this.datatypeProperty = datatypeProperty;
    }
}
