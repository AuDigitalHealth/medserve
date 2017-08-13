package online.medserve.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hl7.fhir.dstu3.model.BackboneElement;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Reference;

import ca.uhn.fhir.model.api.annotation.Block;
import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.api.annotation.Extension;
import ca.uhn.fhir.util.ElementUtil;

@Block
public class MedicationParentExtension extends BackboneElement implements ParentExtendedElement {

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
     * It is important to override the isEmpty() method, adding a check for any newly added fields.
     */
    @Override
    public boolean isEmpty() {
        return super.isEmpty()
                && ElementUtil.isEmpty(parentMedication, medicationResourceType, parentMedicationResources);
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
        return copy;
    }

}
