package au.gov.digitalhealth.medserve.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hl7.fhir.dstu3.model.BackboneElement;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.Enumeration;
import org.hl7.fhir.dstu3.model.Medication.MedicationStatus;
import org.hl7.fhir.dstu3.model.Reference;

import ca.uhn.fhir.model.api.annotation.Block;
import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.api.annotation.Extension;
import ca.uhn.fhir.util.ElementUtil;

@Block
public class MedicationParentExtension extends BackboneElement
        implements ParentExtendedElement, ResourceWithHistoricalAssociations {

    private static final long serialVersionUID = 1L;

    /**
     * A reference to a parent medication which represents an abstraction of the extended medication
     */
    @Child(name = "parentMedication", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/parentMedication", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "A medication resource that represents an abstraction of the extended medication resource")
    private Reference parentMedication;

    /**
     * A code that indicates the level of abstraction of the referenced abstract medication
     */
    @Child(name = "medicationResourceType", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/medicationResourceType", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "A code that indicates what level of abstraction the abstract medication represents")
    private Coding medicationResourceType;

    @Child(name = "parentMedicationResources", min = 0, max = Child.MAX_UNLIMITED, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/parentMedicationResources", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "A collections of medication resources that represent an abstraction of this medication resource")
    private List<MedicationParentExtension> parentMedicationResources;

    /**
     * Status of the referenced medication
     */
    @Child(name = "medicationResourceReferenceStatus", min = 1, max = 1, summary = true)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/medicationResourceReferenceStatus", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "Status of the referenced medication")
    private Enumeration<MedicationStatus> medicationResourceStatus;

    /**
     * The date the underlying definition of the concept or its descriptions were last changed.
     */
    @Child(name = "lastModified", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/lastModified", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "The date the underlying definition of the concept or its descriptions were last changed.")
    private DateType lastModified;

    /**
     * Replacement resources for this resource
     */
    @Child(name = "isReplacedByResources", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/isReplacedByResources", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "Replacement resources for this resource")
    private List<IsReplacedByExtension> isReplacedByResources;

    /**
     * Resources that this resource has replaced
     */
    @Child(name = "replacesResources", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/replacesResources", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "Resources that this resource has replaced")
    private List<ReplacesResourceExtension> replacesResources;

    /**
     * It is important to override the isEmpty() method, adding a check for any newly added fields.
     */
    @Override
    public boolean isEmpty() {
        return super.isEmpty()
                && ElementUtil.isEmpty(parentMedication, medicationResourceType, parentMedicationResources,
                    medicationResourceStatus, lastModified, isReplacedByResources, replacesResources);
    }

    public Reference getParentMedication() {
        if (parentMedication == null) {
            parentMedication = new Reference();
        }
        return parentMedication;
    }

    public void setParentMedication(Reference parentMedication) {
        this.parentMedication = parentMedication;
    }

    public Coding getMedicationResourceType() {
        if (medicationResourceType == null) {
            medicationResourceType = new Coding();
        }
        return medicationResourceType;
    }

    public void setMedicationResourceType(Coding medicationResourceType) {
        this.medicationResourceType = medicationResourceType;
    }

    @Override
    public Collection<MedicationParentExtension> getParentMedicationResources() {
        if (parentMedicationResources == null) {
            parentMedicationResources = new ArrayList<>();
        }
        return parentMedicationResources;
    }

    @Override
    public void setParentMedicationResources(List<MedicationParentExtension> parentMedicationResources) {
        this.parentMedicationResources = parentMedicationResources;
    }

    @Override
    public void addParentMedicationResources(MedicationParentExtension parentMedicationResource) {
        getParentMedicationResources().add(parentMedicationResource);
    }

    @Override
    public MedicationParentExtension copy() {
        MedicationParentExtension copy = new MedicationParentExtension();
        copy.parentMedication = parentMedication;
        copy.medicationResourceType = medicationResourceType;
        copy.medicationResourceStatus = medicationResourceStatus;
        copy.parentMedicationResources = parentMedicationResources;
        copy.lastModified = lastModified;
        copy.isReplacedByResources = new ArrayList<>();
        copy.isReplacedByResources.addAll(isReplacedByResources);
        copy.replacesResources = new ArrayList<>();
        copy.replacesResources.addAll(replacesResources);
        return copy;
    }

    public Enumeration<MedicationStatus> getMedicationResourceStatus() {
        return medicationResourceStatus;
    }

    public void setMedicationResourceStatus(Enumeration<MedicationStatus> medicationResourceStatus) {
        this.medicationResourceStatus = medicationResourceStatus;
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

    @Override
    public List<IsReplacedByExtension> getReplacementResources() {
        if (isReplacedByResources == null) {
            isReplacedByResources = new ArrayList<>();
        }
        return isReplacedByResources;
    }

    @Override
    public void setReplacementResources(List<IsReplacedByExtension> isReplacedByResources) {
        this.isReplacedByResources = isReplacedByResources;
    }

    @Override
    public List<ReplacesResourceExtension> getReplacedResources() {
        if (replacesResources == null) {
            replacesResources = new ArrayList<>();
        }
        return replacesResources;
    }

    @Override
    public void setReplacedResources(List<ReplacesResourceExtension> replacesResources) {
        this.replacesResources = replacesResources;
    }

}
