package online.medserve.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.api.annotation.Extension;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.util.ElementUtil;

@ResourceDef(name = "Medication", id = "extendedMedication", profile = ExtendedMedication.PROFILE_URL_BASE
        + "Profile/ExtendedMedication")
public class ExtendedMedication extends Medication implements IBaseResource, ParentExtendedElement {

    public static final String PROFILE_URL_BASE = "http://medserve.online/fhir/";
    private static final long serialVersionUID = 1L;

    @Child(name = "parentMedicationResources", min = 0, max = Child.MAX_UNLIMITED, summary = false)
    @Extension(url = PROFILE_URL_BASE
            + "StructureDefinition/parentMedicationResources", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "A collections of medication resources that represent an abstraction of this medication resource")
    private List<MedicationParentExtension> parentMedicationResources;

    /**
     * A code that indicates the level of abstraction of the medication
     */
    @Child(name = "medicationResourceType", min = 1, max = 1, summary = true)
    @Extension(url = PROFILE_URL_BASE
            + "StructureDefinition/medicationResourceType", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "A code that indicates what level of abstraction the medication represents")
    private Coding medicationResourceType;

    /**
     * A CodeableConcept for the medication brand
     */
    @Child(name = "brand", min = 0, max = 1, summary = false)
    @Extension(url = PROFILE_URL_BASE + "StructureDefinition/brand", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "A CodeableConcept to identify the brand of the medication if it is branded")
    private CodeableConcept brand;

    /**
     * The code system that was the source of this {@link ExtendedMedication} resource.
     */
    @Child(name = "sourceCodeSystem", min = 1, max = 1, summary = false)
    @Extension(url = PROFILE_URL_BASE
            + "StructureDefinition/sourceCodeSystem", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "The code system that was the source of this {@link ExtendedMedication} resource.")
    private MedicationSourceExtension sourceCodeSystem;

    /**
     * Subsidy
     */
    @Child(name = "subsidies", min = 0, max = 1, summary = false)
    @Extension(url = PROFILE_URL_BASE + "StructureDefinition/subsidy", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "An extension which captures subsidy coding and associated information for this Medication ")
    private List<SubsidyExtension> subsidies;

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && ElementUtil.isEmpty(medicationResourceType, parentMedicationResources, brand,
            sourceCodeSystem, subsidies);
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

    public Coding getMedicationResourceType() {
        if (medicationResourceType == null) {
            medicationResourceType = new Coding();
        }
        return medicationResourceType;
    }

    public void setMedicationResourceType(Coding medicationResourceType) {
        this.medicationResourceType = medicationResourceType;
    }

    public CodeableConcept getBrand() {
        return brand;
    }

    public void setBrand(CodeableConcept brand) {
        this.brand = brand;
    }

    public List<SubsidyExtension> getSubsidies() {
        if (subsidies == null) {
            subsidies = new ArrayList<>();
        }
        return subsidies;
    }

    public void addSubsidy(SubsidyExtension subsidy) {
        this.subsidies.add(subsidy);
    }

    public MedicationSourceExtension getSourceCodeSystem() {
        return sourceCodeSystem;
    }

    public void setSourceCodeSystem(MedicationSourceExtension sourceCodeSystem) {
        this.sourceCodeSystem = sourceCodeSystem;
    }

}
