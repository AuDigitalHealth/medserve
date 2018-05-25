package au.gov.digitalhealth.medserve.extension;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Medication.MedicationIngredientComponent;

import ca.uhn.fhir.model.api.annotation.Block;
import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.api.annotation.Extension;
import ca.uhn.fhir.util.ElementUtil;

@Block
public class MedicationIngredientComponentExtension extends MedicationIngredientComponent {

    private static final long serialVersionUID = 1L;

    /**
     * The Basis of Strength Substance that the Quantity being expressed is relative to. This is a concept in medication
     * strengths where the quantity shown may not be measured in terms of the substance indicated. A good example of
     * this is diclofenac diethylammonium which is often referred to as 10mg/g but that quantity is actually relative to
     * diclofenac sodium and the actual quantity of diclofenac diethylammonium is 11.6mg/g. In this case a measurement
     * of diclofenac diethylammonium 10mg/g should have this extension with a coding value referencing diclofenac
     * sodium.
     */
    @Child(name = "basisOfStrengthSubstance", min = 0, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/basisOfStrengthSubstance", definedLocally = false, isModifier = true)
    @Description(shortDefinition = "The Basis of Strength Substance that the quantity is relative to")
    private Coding basisOfStrengthSubstance;

    /**
     * It is important to override the isEmpty() method, adding a check for any newly added fields.
     */
    @Override
    public boolean isEmpty() {
        return super.isEmpty() && ElementUtil.isEmpty(basisOfStrengthSubstance);
    }

    public Coding getBasisOfStrengthSubstance() {
        if (basisOfStrengthSubstance == null) {
            basisOfStrengthSubstance = new Coding();
        }
        return basisOfStrengthSubstance;
    }

    public void setBasisOfStrengthSubstance(Coding basisOfStrengthSubstance) {
        this.basisOfStrengthSubstance = basisOfStrengthSubstance;
    }

}
