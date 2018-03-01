package online.medserve.extension;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.Enumeration;
import org.hl7.fhir.dstu3.model.Medication.MedicationStatus;
import org.hl7.fhir.dstu3.model.Medication.MedicationStatusEnumFactory;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Narrative.NarrativeStatus;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Ratio;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Substance.FHIRSubstanceStatus;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.dstu3.model.codesystems.SubstanceStatus;
import org.hl7.fhir.dstu3.model.codesystems.SubstanceStatusEnumFactory;
import org.junit.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import online.medserve.FhirCodeSystemUri;

public class TestMarshalling {

    private static final String AMT_VERSION =
            "http://snomed.info/sct?version=http%3A%2F%2Fsnomed.info%2Fsct%2F32506021000036107%2Fversion%2F20180131";
    private static Logger logger = LogManager.getLogger();
    private IParser parser;

    public TestMarshalling() {
        parser = FhirContext.forDstu3().newJsonParser();
        parser.setPrettyPrint(false);
    }

    @Test
    public void testExtendedSubstanceNoReplacements() {
        ExtendedSubstance substance = createSubstanceResource("2352145", "Some substance");

        substance.addIngredient().setSubstance(getSubstanceReference("23462345", "some other substance"));

        parser.encodeResourceToString(substance);

        logger.info(parser.encodeResourceToString(substance));

        logger.info(parser.parseResource(parser.encodeResourceToString(substance)));

    }

    @Test
    public void testExtendedSubstanceReplacements() {
        ExtendedSubstance substance = createSubstanceResource("2352145", "Some substance");

        substance.addIngredient().setSubstance(getSubstanceReference("23462345", "some other substance"));

        substance.getReplacesResources().add(getSubstanceReference("235", "Retired substance"));

        substance.getIsReplacedByResources().add(getSubstanceReference("346", "replacement substance"));

        parser.encodeResourceToString(substance);

        logger.info(parser.encodeResourceToString(substance));
        // logger.info(parser.parseResource(parser.encodeResourceToString(substance)));

    }

    private ExtendedSubstance createSubstanceResource(String code, String display) {
        ExtendedSubstance substance = new ExtendedSubstance();
        substance.setId(code);
        Narrative narrative = new Narrative();
        narrative.setStatus(NarrativeStatus.GENERATED);
        narrative.setDivAsString("<div><p>" + display + "</p></div>");
        substance.setText(narrative);
        substance.setSourceCodeSystem(
            new SourceCodeSystemExtension(new UriType(FhirCodeSystemUri.SNOMED_CT_SYSTEM_URI.getUri()),
                new StringType(AMT_VERSION)));
        substance.setCode(getCodeableConcept(code, display));
        substance.setStatus(FHIRSubstanceStatus.ACTIVE);
        substance.setLastModified(new DateType(new Date()));

        return substance;
    }

    @Test
    public void testProductExtendedMedicationNoParentsNoReplacements() {
        ExtendedMedication medication = createMedicationResource("66681000036100",
            "aluminium chlorohydrate + benzoin Sumatra + dimeticone-350 + liquid paraffin + zinc oxide", false);

        parser.encodeResourceToString(medication);
        medication = createMedicationResource("66681000036100",
            "aluminium chlorohydrate + benzoin Sumatra + dimeticone-350 + liquid paraffin + zinc oxide", true);

        parser.encodeResourceToString(medication);

        logger.info(parser.encodeResourceToString(medication));
        // logger.info(parser.parseResource(parser.encodeResourceToString(medication)));

    }

    @Test
    public void testProductExtendedMedicationParents() {
        ExtendedMedication medication = createMedicationResource("66681000036100",
            "aluminium chlorohydrate + benzoin Sumatra + dimeticone-350 + liquid paraffin + zinc oxide", false);

        ExtendedMedicationReference medicationReference = getMedicationReference("asfdasd", "adfasd");
        medicationReference.addParentMedicationResource(getMedicationReference("asdfsa", "asdfsd"));
        medication.getPackage().addContent().setItem(medicationReference);

        medication.addParentMedicationResource(getMedicationReference("asdfa", "adfasdf"));

        medication.getParentMedicationResources()
            .iterator()
            .next()
            .addParentMedicationResource(getMedicationReference("asdfa", "adfasdf"));

        parser.encodeResourceToString(medication);

        medication = createMedicationResource("66681000036100",
            "aluminium chlorohydrate + benzoin Sumatra + dimeticone-350 + liquid paraffin + zinc oxide", true);

        medicationReference = getMedicationReference("asfdasd", "adfasd");
        medicationReference.addParentMedicationResource(getMedicationReference("asdfsa", "asdfsd"));
        medication.getPackage().addContent().setItem(medicationReference);

        medication.addParentMedicationResource(getMedicationReference("asdfa", "adfasdf"));

        medication.getParentMedicationResources()
            .iterator()
            .next()
            .addParentMedicationResource(getMedicationReference("asdfa", "adfasdf"));

        parser.encodeResourceToString(medication);
        logger.info(parser.encodeResourceToString(medication));
        // logger.info(parser.parseResource(parser.encodeResourceToString(medication)));

    }

    private ExtendedMedication createMedicationResource(String id, String display, boolean manufacturer) {
        ExtendedMedication medication = new ExtendedMedication();

        medication.setSourceCodeSystem(
            new SourceCodeSystemExtension(new UriType(FhirCodeSystemUri.SNOMED_CT_SYSTEM_URI.getUri()),
                new StringType(AMT_VERSION)));

        medication.setId(id);
        Narrative narrative = new Narrative();
        narrative.setStatus(NarrativeStatus.GENERATED);
        narrative.setDivAsString("<div><p>" + display + "</p></div>");
        medication.setText(narrative);

        // addHistoicalAssociations(concept, medication, new HashSet<>(), createdResources);

        medication.setLastModified(new DateType(new Date()));

        medication.setCode(getCodeableConcept(id, display));

        medication.setStatus(MedicationStatus.ACTIVE);

        medication.setMedicationResourceType(MedicationType.UnbrandedProduct.getCode());

        // addParentExtensions(concept, medication, new HashSet<>(), createdResources);

        medication.setIsBrand(false);

        if (manufacturer) {
        medication.setManufacturer(new Reference("Organization/123"));
        }
        medication.getCode().addCoding().setSystem(FhirCodeSystemUri.TGA_URI.getUri()).setCode("TGAID");

        medication.setBrand(getCodeableConcept("12345", "Some brand"));

        MedicationIngredientComponentExtension ingredient = new MedicationIngredientComponentExtension();

        medication.addIngredient(ingredient);

        ingredient.setItem(getSubstanceReference("134556", "Some substance"));

        ingredient.setBasisOfStrengthSubstance(getCoding("135346", "BoSS substance"));

        Ratio value = new Ratio();

        Quantity denominator = new Quantity(1L);

        denominator.setCode("234523");
        denominator.setUnit("some unit");
        denominator.setSystem(FhirCodeSystemUri.SNOMED_CT_SYSTEM_URI.getUri());

        value.setDenominator(denominator);

        Quantity numerator = new Quantity(234.54D);
        numerator.setCode("2353426");
        numerator.setUnit("some unit");
        numerator.setSystem(FhirCodeSystemUri.SNOMED_CT_SYSTEM_URI.getUri());
        value.setNumerator(numerator);

        ingredient.setAmount(value);

        medication.setForm(getCodeableConcept("234521", "some form"));
        return medication;
    }

    private Coding getCoding(String code, String display) {
        return new Coding(FhirCodeSystemUri.SNOMED_CT_SYSTEM_URI.getUri(), code, display);
    }

    private ExtendedMedicationReference getMedicationReference(String id, String display) {
        ExtendedMedicationReference reference =
                new ExtendedMedicationReference("Medication/" + id);
        reference.setDisplay(display);
        // addParentExtensions(concept, reference, addedConcepts, createdResources);

        reference.setType(MedicationType.BrandedPackage.getCode());
        reference.setStatus(
            new Enumeration<MedicationStatus>(new MedicationStatusEnumFactory(), MedicationStatus.ACTIVE));
        reference.setLastModified(new DateType(new Date()));

        // addHistoicalAssociations(concept, reference, new HashSet<>(), createdResources);

        return reference;
    }

    private ExtendedSubstanceReference getSubstanceReference(String id, String display) {
        ExtendedSubstanceReference reference =
                new ExtendedSubstanceReference("Substance/" + id);
        reference.setDisplay(display);
        reference.setStatus(
            new Enumeration<SubstanceStatus>(new SubstanceStatusEnumFactory(), SubstanceStatus.ACTIVE));
        reference.setLastModified(new DateType(new Date()));

        // addHistoicalAssociations(concept, reference, new HashSet<>(), createdResources);

        return reference;
    }

    private CodeableConcept getCodeableConcept(String code, String display) {
        CodeableConcept codeableConcept = new CodeableConcept();
        Coding coding = new Coding(FhirCodeSystemUri.SNOMED_CT_SYSTEM_URI.getUri(), code, display);
        codeableConcept.addCoding(coding);
        return codeableConcept;
    }

}
