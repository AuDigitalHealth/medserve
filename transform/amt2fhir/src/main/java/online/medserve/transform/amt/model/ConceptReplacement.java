package online.medserve.transform.amt.model;

import java.util.Date;

public class ConceptReplacement {
    Long type;
    Concept concept;
    Date replacementDate;

    public ConceptReplacement(Long type, Concept concept, Date replacementDate) {
        super();
        this.type = type;
        this.concept = concept;
        this.replacementDate = replacementDate;
    }

    public Long getType() {
        return type;
    }

    public Concept getConcept() {
        return concept;
    }

    public Date getReplacementDate() {
        return replacementDate;
    }
}
