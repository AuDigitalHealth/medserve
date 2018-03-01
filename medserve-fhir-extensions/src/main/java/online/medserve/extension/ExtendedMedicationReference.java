package online.medserve.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.Enumeration;
import org.hl7.fhir.dstu3.model.Medication.MedicationStatus;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.ICompositeType;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.api.annotation.Extension;
import ca.uhn.fhir.util.ElementUtil;

@DatatypeDef(name = "ExtendedMedicationReference", isSpecialization = true, profileOf = Reference.class)
public class ExtendedMedicationReference extends Reference
        implements ICompositeType, ParentExtendedElement, ElementWithHistoricalMedicationReferences {

    public static final String PROFILE_URL_BASE = "http://medserve.online/fhir/";
    private static final long serialVersionUID = 1L;

    @Child(name = "parentMedicationResources", min = 0, max = Child.MAX_UNLIMITED, summary = false)
    @Extension(url = PROFILE_URL_BASE
            + "StructureDefinition/ExtendedMedication/parentMedicationResources", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "A collections of medication resources that represent an abstraction of this medication resource")
    private List<ExtendedMedicationReference> parentMedicationResources;

    /**
     * A code that indicates the level of abstraction of the medication
     */
    @Child(name = "type", min = 1, max = 1, summary = true)
    @Extension(url = PROFILE_URL_BASE
            + "StructureDefinition/medicationResourceType", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "A code that indicates what level of abstraction the medication represents")
    private Coding type;

    /**
     * Status of the referenced medication
     */
    @Child(name = "status", min = 1, max = 1, summary = true)
    @Extension(url = PROFILE_URL_BASE
            + "StructureDefinition/ExtendedMedicationReference/status", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "Status of the referenced medication")
    private Enumeration<MedicationStatus> status;

    /**
     * The date the underlying definition of the concept or its descriptions were last changed.
     */
    @Child(name = "lastModified", min = 1, max = 1, summary = false)
    @Extension(url = PROFILE_URL_BASE
            + "StructureDefinition/lastModified", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "The date the underlying definition of the concept or its descriptions were last changed.")
    private DateType lastModified;

    /**
     * A reference to a resource the resource replaces or is replaced by - this is contextual depending upon where this
     * reference appears
     */
    @Child(name = "historicallyRelatedResource", min = 0, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/ExtendedReference/historicallyRelatedResource", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "A reference to a resource the resource replaces or is replaced by - this is contextual depending upon where this reference appears")
    private ExtendedMedicationReference historicallyRelatedResource;

    /**
     * The date this resource replaced, or was replaced by, the historicallyRelatedResource.
     */
    @Child(name = "replacementDate", min = 0, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/ExtendedReference/replacementDate", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "The date this resource replaced, or was replaced by, the historicallyRelatedResource.")
    private DateType replacementDate;

    /**
     * The type of replacement
     */
    @Child(name = "replacementType", min = 0, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/ExtendedReference/replacementType", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "The type of the replacement")
    private Coding replacementType;

    /**
     * True if the referenced resource replaces the historicallyRelatedResource or false if it is replaced by the
     * historicallyRelatedResource
     */
    @Child(name = "replacesHistoricallyRelatedResource", min = 0, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/ExtendedReference/replacesHistoricallyRelatedResource", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "True if the referenced resource replaces the historicallyRelatedResource or false if it is replaced by the historicallyRelatedResource")
    private BooleanType replacesHistoricallyRelatedResource;

    /**
     * Replacement resources for this resource
     */
    @Child(name = "isReplacedByResources", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/isReplacedByResources", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "Replacement resources for this resource")
    private List<ExtendedMedicationReference> isReplacedByResources;

    /**
     * Resources that this resource has replaced
     */
    @Child(name = "replacesResources", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/replacesResources", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "Resources that this resource has replaced")
    private List<ExtendedMedicationReference> replacesResources;

    public ExtendedMedicationReference() {}

    public ExtendedMedicationReference(String theReference) {
        super(theReference);
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && ElementUtil.isEmpty(type, status, lastModified, historicallyRelatedResource,
            replacementDate, replacesHistoricallyRelatedResource,
            isReplacedByResources, replacesResources);
    }

    @Override
    public Collection<ExtendedMedicationReference> getParentMedicationResources() {
        if (parentMedicationResources == null) {
            parentMedicationResources = new ArrayList<>();
        }
        return parentMedicationResources;
    }

    public void setParentMedicationResources(List<ExtendedMedicationReference> parentMedicationResources) {
        this.parentMedicationResources = parentMedicationResources;
    }

    @Override
    public void addParentMedicationResource(ExtendedMedicationReference parentMedicationResource) {
        getParentMedicationResources().add(parentMedicationResource);
    }

    public Coding getType() {
        if (type == null) {
            type = new Coding();
        }
        return type;
    }

    public void setType(Coding type) {
        this.type = type;
    }

    public Enumeration<MedicationStatus> getStatus() {
        return status;
    }

    public void setStatus(Enumeration<MedicationStatus> status) {
        this.status = status;
    }

    public DateType getLastModified() {
        if (lastModified == null) {
            lastModified = new DateType();
        }
        return lastModified;
    }

    public void setLastModified(DateType lastModified) {
        this.lastModified = lastModified;
    }

    public ExtendedMedicationReference getHistoricallyRelatedResource() {
        return historicallyRelatedResource;
    }

    public void setHistoricallyRelatedResource(ExtendedMedicationReference historicallyRelatedResource) {
        this.historicallyRelatedResource = historicallyRelatedResource;
    }

    public DateType getReplacementDate() {
        return replacementDate;
    }

    public void setReplacementDate(DateType replacementDate) {
        this.replacementDate = replacementDate;
    }

    public BooleanType getReplacesHistoricallyRelatedResource() {
        return replacesHistoricallyRelatedResource;
    }

    public void setReplacesHistoricallyRelatedResource(BooleanType replacesHistoricallyRelatedResource) {
        this.replacesHistoricallyRelatedResource = replacesHistoricallyRelatedResource;
    }

    /*
     * (non-Javadoc)
     * 
     * @see online.medserve.extension.ElementWithHistoricalMedicationReferences#getIsReplacedByResources()
     */
    @Override
    public List<ExtendedMedicationReference> getIsReplacedByResources() {
        if (isReplacedByResources == null) {
            isReplacedByResources = new ArrayList<>();
        }
        return isReplacedByResources;
    }

    public void setIsReplacedByResources(List<ExtendedMedicationReference> isReplacedByResources) {
        this.isReplacedByResources = isReplacedByResources;
    }

    /*
     * (non-Javadoc)
     * 
     * @see online.medserve.extension.ElementWithHistoricalMedicationReferences#getReplacesResources()
     */
    @Override
    public List<ExtendedMedicationReference> getReplacesResources() {
        if (replacesResources == null) {
            replacesResources = new ArrayList<>();
        }
        return replacesResources;
    }

    public void setReplacesResources(List<ExtendedMedicationReference> replacesResources) {
        this.replacesResources = replacesResources;
    }

    public Coding getReplacementType() {
        return replacementType;
    }

    public void setReplacementType(Coding replacementType) {
        this.replacementType = replacementType;
    }

}
