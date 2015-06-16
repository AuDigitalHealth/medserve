package amt2fhir;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;

public class Concept {

    public static final String SNOMED_CT_SYSTEM_URI = "http://snomed.info/sct";
    private long id;
    private String fullSpecifiedName;
    private String preferredTerm;
    private Map<Long, Set<Relationship>> relationshipGroups = new HashMap<>();
    private Set<Concept> parents = new HashSet<>();
    private CodeableConceptDt codableConceptDt;
    private CodingDt codingDt;

    public Concept(long id) {
        this.id = id;
    }

    public void addParent(Concept concept) {
        parents.add(concept);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFullSpecifiedName() {
        return fullSpecifiedName;
    }

    public void setFullSpecifiedName(String fullSpecifiedName) {
        this.fullSpecifiedName = fullSpecifiedName;
    }

    public String getPreferredTerm() {
        return preferredTerm;
    }

    public void setPreferredTerm(String preferredTerm) {
        this.preferredTerm = preferredTerm;
    }

    public Map<Long, Set<Relationship>> getRelationshipGroups() {
        return relationshipGroups;
    }

    public void setRelationshipGroups(Map<Long, Set<Relationship>> relationshipGroups) {
        this.relationshipGroups = relationshipGroups;
    }

    public Set<Concept> getParents() {
        return parents;
    }

    public void setParents(Set<Concept> parents) {
        this.parents = parents;
    }

    public CodeableConceptDt toCodeableConceptDt() {
        if (codableConceptDt == null) {
            codableConceptDt = new CodeableConceptDt();
            codableConceptDt.setCoding(Arrays.asList(toCodingDt()));
        }
        return codableConceptDt;
    }

    public CodingDt toCodingDt() {
        if (codingDt == null) {
            codingDt = new CodingDt(SNOMED_CT_SYSTEM_URI, Long.toString(getId()));
            codingDt.setDisplay(getPreferredTerm());
        }
        return codingDt;
    }

    public Concept getSingleDestination(AttributeType relationshipType) {
        Relationship relationship = getRelationshipGroups().values()
            .stream()
            .flatMap(list -> list.stream())
            .filter(rel -> rel.getType().equals(relationshipType))
            .findFirst()
            .orElse(null);
        return relationship == null ? null : relationship.getDestination();
    }

    public Collection<Concept> getMultipleDestinations(AttributeType relationshipType) {
        return getRelationshipGroups().values()
            .stream()
            .flatMap(list -> list.stream())
            .filter(rel -> rel.getType().equals(relationshipType))
            .map(r -> r.getDestination())
            .collect(Collectors.toSet());
    }

    public Collection<Relationship> getRelationships(AttributeType type) {
        return getRelationshipGroups().values()
            .stream()
            .flatMap(list -> list.stream())
            .filter(r -> r.getType().equals(type))
            .collect(Collectors.toSet());
    }

    public Collection<Collection<Relationship>> getRelationshipGroupsContaining(AttributeType type) {
        return getRelationshipGroups().values()
            .stream()
            .filter(l -> l.stream().anyMatch(r -> r.getType().equals(type)))
            .collect(Collectors.toSet());
    }

    public boolean hasParent(AmtConcept amtConcept) {
        return parents.stream().anyMatch(p -> amtConcept.getId() == p.getId());
    }
}
