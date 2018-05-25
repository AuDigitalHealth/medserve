package online.medserve.extension;

import org.hl7.fhir.dstu3.model.BackboneElement;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Reference;

import ca.uhn.fhir.model.api.annotation.Block;
import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.api.annotation.Extension;
import ca.uhn.fhir.util.ElementUtil;

@Block
public class GeneralizedMedication extends BackboneElement {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * A coding for the subsidy from the subsidy provider
     */
    @Child(name = "medication", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/medication", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "Reference to the more general medication")
    private Reference medication;
    /**
     * A code that indicates the level of abstraction of the medication
     */
    @Child(name = "medicationResourceType", min = 1, max = 1, summary = true)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/medicationResourceType", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "A code that indicates what level of abstraction the medication represents")
    private Coding medicationResourceType;

    public GeneralizedMedication() {
        super();
    }

    public GeneralizedMedication(Reference medication, Coding medicationResourceType) {
        super();
        this.medication = medication;
        this.medicationResourceType = medicationResourceType;
    }

    /**
     * It is important to override the isEmpty() method, adding a check for any newly added fields.
     */
    @Override
    public boolean isEmpty() {
        return super.isEmpty() && ElementUtil.isEmpty(medication, medicationResourceType);
    }

    @Override
    public GeneralizedMedication copy() {
        GeneralizedMedication copy = new GeneralizedMedication();
        copy.medication = medication;
        copy.medicationResourceType = medicationResourceType;
        return copy;
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

    public Reference getMedication() {
        if (medication == null) {
            medication = new Reference();
        }
        return medication;
    }

    public void setMedication(Reference medication) {
        this.medication = medication;
    }

}
