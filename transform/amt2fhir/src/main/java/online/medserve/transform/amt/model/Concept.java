package online.medserve.transform.amt.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Medication.MedicationStatus;

import online.medserve.extension.MedicationType;
import online.medserve.transform.amt.enumeration.AmtConcept;
import online.medserve.transform.amt.enumeration.AttributeType;
import online.medserve.transform.util.FhirCodeSystemUri;

public class Concept {

    private long id;
    private String fullSpecifiedName;
    private String preferredTerm;
    private Map<Long, Set<Relationship>> relationshipGroups = new HashMap<>();
    private Map<Long, Concept> parents = new HashMap<>();
    private CodeableConcept codableConcept;
    private Coding coding;
    private Map<Long, Concept> ancestors = new HashMap<>();
    private boolean active;
    private Date lastModified;
    private Date conceptLastModified;
    private Set<Subsidy> subsidies = new HashSet<>();
    private Manufacturer manufacturer;
    private List<ImmutableTriple<Long, Concept, Date>> replacementConcepts;
    private List<ImmutableTriple<Long, Concept, Date>> replacedConcepts;
    private Map<String, Concept> leafAncestor = new HashMap<>();

    public Concept(long id, boolean active, Date conceptLastModified) {
        this.id = id;
        this.active = active;
        this.conceptLastModified = conceptLastModified;
        this.lastModified = conceptLastModified;
    }

    public void addParent(Concept concept) {
        parents.put(concept.getId(), concept);
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

    public CodeableConcept toCodeableConcept() {
        if (codableConcept == null) {
            codableConcept = new CodeableConcept();
            List<Coding> list = new ArrayList<>();
            list.add(toCoding());
            codableConcept.setCoding(list);
        }
        return codableConcept;
    }

    public Coding toCoding() {
        if (coding == null) {
            coding = new Coding(FhirCodeSystemUri.SNOMED_CT_SYSTEM_URI.getUri(), Long.toString(getId()),
                getPreferredTerm());
        }
        return coding;
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

    public boolean hasOneMatchingParent(AmtConcept... amtConcept) {
        for (AmtConcept parent : amtConcept) {
            if (parents.containsKey(parent.getId())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasParent(AmtConcept amtConcept) {
        return parents.containsKey(amtConcept.getId());
    }

    public boolean hasParent(Concept concept) {
        return parents.containsKey(concept.getId());
    }

    public Map<Long, Concept> getParents() {
        return parents;
    }

    public String toConceptReference() {
        return getId() + "|" + getPreferredTerm() + "|";
    }

    public boolean hasSameDestinations(Concept source, AttributeType relType) {
        Collection<Concept> setA = getMultipleDestinations(relType);
        Collection<Concept> setB = source.getMultipleDestinations(relType);
        return setA.containsAll(setB) && setB.containsAll(setA);
    }

    public void addAncestors(Map<Long, Concept> map) {
        ancestors.putAll(map);
    }

    public boolean hasAtLeastOneMatchingAncestor(AmtConcept... concepts) {
        for (AmtConcept amtConcept : concepts) {
            if (ancestors.containsKey(amtConcept.getId())) {
                return true;
            }
        }
        return false;
    }

    public Collection<Concept> getAncestors(AmtConcept concept) {
        Collection<Concept> result = new ArrayList<>();
        for (Concept ancestor : ancestors.values()) {
            if (ancestor.hasAtLeastOneMatchingAncestor(concept)) {
                result.add(ancestor);
            }
        }
        return result;
    }

    public Concept getLeafAncestor(AmtConcept parentType, AmtConcept currentType) {
        String cacheKey = parentType.getDisplay() + " " + currentType.getDisplay();
        if (!leafAncestor.containsKey(cacheKey)) {
            Set<Concept> leafParents = new HashSet<>();

            leafParents.addAll(getAncestors(parentType).stream()
                .filter(p -> !AmtConcept.isEnumValue(p.getId()))
                .filter(p -> p.hasAtLeastOneMatchingAncestor(parentType))
                .filter(p -> !p.hasAtLeastOneMatchingAncestor(currentType))
                .collect(Collectors.toSet()));

            Set<Concept> redunantAncestors =
                    leafParents.stream().flatMap(p -> p.getAncestors(parentType).stream()).collect(Collectors.toSet());

            leafParents.removeAll(redunantAncestors);

            if (leafParents.size() > 1) {
                throw new RuntimeException("Concept " + this + " has more than one ancestor of type " + parentType);
            }

            leafAncestor.put(cacheKey, leafParents.iterator().next());
        }
        return leafAncestor.get(cacheKey);
    }

    @Override
    public String toString() {
        return "Concept [id=" + id + ", fullSpecifiedName=" + fullSpecifiedName + ", parents=" + parents + "]";
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
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
        if (id != other.id)
            return false;
        return true;
    }

    public void setManufacturer(Manufacturer manufacturer) {
        if (this.manufacturer != null && !this.manufacturer.equals(manufacturer)) {
            throw new RuntimeException("More than one manufacturer for the same product! " + manufacturer + " and "
                    + this.manufacturer + " for " + this);
        } else {
            this.manufacturer = manufacturer;
        }
    }

    public Manufacturer getManufacturer() {
        return manufacturer;
    }

    public void addSubsidy(Subsidy subsidy) {
        if (!this.subsidies.contains(subsidy)) {
            this.subsidies.add(subsidy);
        }
    }

    public Set<Subsidy> getSubsidies() {
        return subsidies;
    }

    public AmtConcept getAmtType() {
        if (hasAtLeastOneMatchingAncestor(AmtConcept.CTPP)) {
            return AmtConcept.CTPP;
        } else if (hasAtLeastOneMatchingAncestor(AmtConcept.TPP)) {
            return AmtConcept.TPP;
        } else if (hasAtLeastOneMatchingAncestor(AmtConcept.TPUU)) {
            return AmtConcept.TPUU;
        } else if (hasAtLeastOneMatchingAncestor(AmtConcept.MPP)) {
            return AmtConcept.MPP;
        } else if (hasAtLeastOneMatchingAncestor(AmtConcept.MPUU)) {
            return AmtConcept.MPUU;
        } else if (hasAtLeastOneMatchingAncestor(AmtConcept.MP)) {
            return AmtConcept.MP;
        } else if (hasAtLeastOneMatchingAncestor(AmtConcept.SUBSTANCE)) {
            return AmtConcept.SUBSTANCE;
        }
        throw new RuntimeException("Concept " + this + " is not of a known AmtConcept type");
    }

    public MedicationType getMedicationType() {
        switch (getAmtType()) {
            case CTPP:
                return MedicationType.BrandedPackgeContainer;
            case TPP:
                return MedicationType.BrandedPackage;
            case TPUU:
                return MedicationType.BrandedProductStrengthForm;
            case MPP:
                return MedicationType.UnbrandedPackage;
            case MPUU:
                return MedicationType.UnbrandedProductStrengthForm;
            case MP:
                return MedicationType.UnbrandedProduct;
            default:
                throw new RuntimeException("Concept " + this + " is not of a known AmtConcept type");
        }
    }

    public Date getLastModified() {
        return lastModified;
    }

    public Date getConceptLastModified() {
        return conceptLastModified;
    }

    public void updateLastModified(Date effectiveTime) {
        if (effectiveTime.after(lastModified)) {
            lastModified = effectiveTime;
        }
    }

    public MedicationStatus getStatus() {
        return active ? MedicationStatus.ACTIVE : MedicationStatus.ENTEREDINERROR;
    }

    public void addReplacementConcept(long type, Concept replacement, Date date) {
        if (replacementConcepts == null) {
            replacementConcepts = new ArrayList<>();
        }
        replacementConcepts.add(new ImmutableTriple<Long, Concept, Date>(type, replacement, date));
    }

    public void addReplacedConcept(long type, Concept retiredConcept, Date date) {
        if (replacedConcepts == null) {
            replacedConcepts = new ArrayList<>();
        }
        replacedConcepts.add(new ImmutableTriple<Long, Concept, Date>(type, retiredConcept, date));
    }

    public List<ImmutableTriple<Long, Concept, Date>> getReplacementConcept() {
        return replacementConcepts;
    }

    public List<ImmutableTriple<Long, Concept, Date>> getReplacedConcept() {
        return replacedConcepts;
    }

    public String getResourceType() {
        switch (getAmtType()) {
            case MP:
            case MPUU:
            case MPP:
            case TP:
            case TPUU:
            case TPP:
            case CTPP:
                return "Medication";
            case SUBSTANCE:
                return "Substance";
            default:
                throw new RuntimeException("Failed to determine resource type for " + this);
        }
    }

    public CodeableConcept getBrand() {
        CodeableConcept brand = null;
        if (hasAtLeastOneMatchingAncestor(AmtConcept.TP)) {
            if (getAncestors(AmtConcept.TP)
                .stream()
                .filter(c -> !AmtConcept.isEnumValue(c.getId()))
                .count() > 1) {
                throw new RuntimeException("more than one TP " + getAncestors(AmtConcept.TP));
            }
            brand = getAncestors(AmtConcept.TP)
                .stream()
                .filter(c -> !AmtConcept.isEnumValue(c.getId()))
                .iterator()
                .next()
                .toCodeableConcept();
        } else if (hasAtLeastOneMatchingAncestor(AmtConcept.TPP)) {
            brand = getSingleDestination(AttributeType.HAS_TP).toCodeableConcept();
        }
        return brand;
    }

}
