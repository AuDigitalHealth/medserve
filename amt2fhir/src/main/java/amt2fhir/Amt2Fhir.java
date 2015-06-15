package amt2fhir;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.util.HashMap;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IValueSetEnumBinder;
import ca.uhn.fhir.model.dstu2.composite.BoundCodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.QuantityDt;
import ca.uhn.fhir.model.dstu2.composite.RatioDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Medication;
import ca.uhn.fhir.model.dstu2.resource.Medication.Product;
import ca.uhn.fhir.model.dstu2.resource.Medication.ProductIngredient;
import ca.uhn.fhir.model.dstu2.resource.Substance;
import ca.uhn.fhir.model.dstu2.valueset.MedicationKindEnum;
import ca.uhn.fhir.model.dstu2.valueset.SubstanceTypeEnum;
import ca.uhn.fhir.parser.IParser;

public class Amt2Fhir {

    public static void main(String args[]) throws IOException, URISyntaxException {
        ConceptCache concepts = new ConceptCache(FileSystems.newFileSystem(
            URI.create("jar:file:" + FileSystems.getDefault().getPath(args[0]).toAbsolutePath().toString()),
            new HashMap<>()));

        concepts.getMps().values().forEach(concept -> createFhirResourceForMp(concept));
    }

    private static void createFhirResourceForMp(Concept concept) {
        Medication medication = new Medication();

        medication.setName(concept.getPreferredTerm());
        medication.setCode(concept.toCodeableConceptDt());
        medication.setIsBrand(false);
        medication.setKind(MedicationKindEnum.PRODUCT);
        Product product = new Product();

        concept.getRelationshipGroups()
            .values()
            .stream()
            .flatMap(list -> list.stream())
            .filter(r -> r.getType().equals(AttributeType.HAS_INTENDED_ACTIVE_INGREDIENT))
            .forEach(r -> addIngredient(product, r));

        IParser parser = FhirContext.forDstu2().newJsonParser();

    }

    private static void addIngredient(Product product, Relationship r) {
        ProductIngredient ingredient = product.addIngredient();
        Substance substance = new Substance();
        ingredient.setItem(new ResourceReferenceDt(substance));

        BoundCodeableConceptDt<SubstanceTypeEnum> substanceCode = new BoundCodeableConceptDt<SubstanceTypeEnum>(
            new IValueSetEnumBinder<SubstanceTypeEnum>() {

                @Override
                public SubstanceTypeEnum fromCodeString(String theCodeString) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String toCodeString(SubstanceTypeEnum theEnum) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String toSystemString(SubstanceTypeEnum theEnum) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public SubstanceTypeEnum fromCodeString(String theCodeString, String theSystemString) {
                    // TODO Auto-generated method stub
                    return null;
                }
            });
        System.out.println("added code " + r.getDestination().getId());

        if (r.getDatatypeProperty() != null) {
            DataTypeProperty d = r.getDatatypeProperty();
            Concept denominatorUnit = d.getUnit()
                .getRelationshipGroups()
                .values()
                .stream()
                .flatMap(list -> list.stream())
                .filter(rel -> rel.getType().equals(AttributeType.HAS_DENOMINATOR_UNITS))
                .findFirst()
                .get()
                .getDestination();
            Concept numeratorUnit = d.getUnit()
                .getRelationshipGroups()
                .values()
                .stream()
                .flatMap(list -> list.stream())
                .filter(rel -> rel.getType().equals(AttributeType.HAS_NUMERATOR_UNITS))
                .findFirst()
                .get()
                .getDestination();

            RatioDt value = new RatioDt();

            QuantityDt denominator = new QuantityDt(1L);

            denominator.setCode(Long.toString(denominatorUnit.getId()));
            denominator.setUnits(denominatorUnit.getPreferredTerm());
            denominator.setSystem(Concept.SNOMED_CT_SYSTEM_URI);
            value.setDenominator(denominator);

            QuantityDt numerator = new QuantityDt(Double.parseDouble(d.getValue()));
            numerator.setCode(Long.toString(numeratorUnit.getId()));
            numerator.setUnits(numeratorUnit.getPreferredTerm());
            numerator.setSystem(Concept.SNOMED_CT_SYSTEM_URI);
            value.setNumerator(numerator);

            ingredient.setAmount(value);
        }
    }

}
