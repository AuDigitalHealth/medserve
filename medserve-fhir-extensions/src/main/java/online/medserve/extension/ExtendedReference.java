package online.medserve.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Reference;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.api.annotation.Extension;
import ca.uhn.fhir.util.ElementUtil;

@DatatypeDef(name = "ExtendedReference", isSpecialization = true, profileOf = Reference.class)
public class ExtendedReference extends Reference implements ParentExtendedElement {

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

    public ExtendedReference(String theReference) {
        super(theReference);
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && ElementUtil.isEmpty(medicationResourceType);
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
}
