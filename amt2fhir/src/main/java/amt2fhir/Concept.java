package amt2fhir;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Concept {

    private long id;
    private String fullSpecifiedName;
    private String preferredTerm;
    private Map<Long, Set<Relationship>> relationshipGroups = new HashMap<>();
    private Set<Concept> parents = new HashSet<>();

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
}
