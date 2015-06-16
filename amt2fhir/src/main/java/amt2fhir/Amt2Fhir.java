package amt2fhir;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.composite.QuantityDt;
import ca.uhn.fhir.model.dstu2.composite.RatioDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Medication;
import ca.uhn.fhir.model.dstu2.resource.Medication.Product;
import ca.uhn.fhir.model.dstu2.resource.Medication.ProductIngredient;
import ca.uhn.fhir.model.dstu2.valueset.MedicationKindEnum;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;

public class Amt2Fhir {

    private static final Logger logger = Logger.getLogger(ConceptCache.class.getCanonicalName());

    private static IParser parser;
    private static FhirValidator validator;

    public static void main(String args[]) throws IOException, URISyntaxException {
        ConceptCache concepts = new ConceptCache(FileSystems.newFileSystem(
            URI.create("jar:file:" + FileSystems.getDefault().getPath(args[0]).toAbsolutePath().toString()),
            new HashMap<>()));

        String basePath = args[1];
        initialiseOutputDirectories(basePath, "mp");
        initialiseOutputDirectories(basePath, "mpuu");
        initialiseOutputDirectories(basePath, "mpp");
        initialiseOutputDirectories(basePath, "tpuu");
        initialiseOutputDirectories(basePath, "tpp");
        initialiseOutputDirectories(basePath, "ctpp");

        parser = FhirContext.forDstu2().newJsonParser();
        parser.setPrettyPrint(true);
        validator = FhirContext.forDstu2().newValidator();

        concepts.getMps().values().forEach(concept -> createAndWriteFhirResource(concept, basePath, "mp"));
        concepts.getMpuus().values().forEach(concept -> createAndWriteFhirResource(concept, basePath, "mpuu"));
        concepts.getTpuus().values().forEach(concept -> createAndWriteFhirResource(concept, basePath, "tpuu"));

    }

    private static void createAndWriteFhirResource(Concept concept, String basePath, String type) {
        Medication medication = createFhirResource(concept);

        ValidationResult result = validator.validateWithResult(medication);

        try {
            if (result.isSuccessful()) {
                Files.write(getSuccessPath(basePath, type).resolve(getFileName(medication)),
                    parser.encodeResourceToString(medication).getBytes(), StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            } else {
                Files.write(getFailPath(basePath, type).resolve(getFileName(medication)),
                    parser.encodeResourceToString(medication).getBytes(), StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            }
        } catch (DataFormatException | IOException e) {
            logger.log(Level.SEVERE, "Failed writing out medication " + parser.encodeResourceToString(medication), e);
        }
    }

    private static String getFileName(Medication medication) {
        String name = medication.getName().replaceAll("/", "_");

        if (name.length() > 255) {
            String code = medication.getCode().getCodingFirstRep().getCode();
            return code + " - " + name.substring(0, 240 - code.length());
        }

        return name;
    }

    private static Medication createFhirResource(Concept concept) {
        Medication medication = new Medication();

        medication.setName(concept.getPreferredTerm());
        medication.setCode(concept.toCodeableConceptDt());
        medication.setIsBrand(concept.hasParent(AmtConcept.TP));
        medication.setKind(MedicationKindEnum.PRODUCT);
        Product product = new Product();
        medication.setProduct(product);

        concept.getRelationshipGroupsContaining(AttributeType.HAS_INTENDED_ACTIVE_INGREDIENT).forEach(
            r -> addIngredient(product, r));

        Concept form = concept.getSingleDestination(AttributeType.HAS_MANUFACTURED_DOSE_FORM);
        if (form != null) {
            product.setForm(form.toCodeableConceptDt());
        }
        return medication;
    }

    private static void addIngredient(Product product, Collection<Relationship> relationships) {
        Relationship iai = relationships.stream()
            .filter(r -> r.getType().equals(AttributeType.HAS_INTENDED_ACTIVE_INGREDIENT))
            .findFirst()
            .get();
        ProductIngredient ingredient = product.addIngredient();
        ResourceReferenceDt substance = new ResourceReferenceDt();

        substance.setReference("Substance?system=" + Concept.SNOMED_CT_SYSTEM_URI + "&id="
            + iai.getDestination().getId());
        substance.setDisplay(iai.getDestination().getPreferredTerm());

        ingredient.setItem(substance);

        Relationship hasBoss = relationships.stream()
            .filter(r -> r.getType().equals(AttributeType.HAS_AUSTRALIAN_BOSS))
            .findFirst()
            .orElse(null);
        if (hasBoss != null) {
            DataTypeProperty d = hasBoss.getDatatypeProperty();
            Concept denominatorUnit = d.getUnit().getSingleDestination(AttributeType.HAS_DENOMINATOR_UNITS);
            Concept numeratorUnit = d.getUnit().getSingleDestination(AttributeType.HAS_NUMERATOR_UNITS);

            RatioDt value = new RatioDt();

            QuantityDt denominator = new QuantityDt(1L);

            if (denominatorUnit != null && !denominatorUnit.getPreferredTerm().equals("each")) {
                denominator.setCode(Long.toString(denominatorUnit.getId()));
                denominator.setUnits(denominatorUnit.getPreferredTerm());
                denominator.setSystem(Concept.SNOMED_CT_SYSTEM_URI);
            }
            value.setDenominator(denominator);

            QuantityDt numerator = new QuantityDt(Double.parseDouble(d.getValue()));
            numerator.setCode(Long.toString(numeratorUnit.getId()));
            numerator.setUnits(numeratorUnit.getPreferredTerm());
            numerator.setSystem(Concept.SNOMED_CT_SYSTEM_URI);
            value.setNumerator(numerator);

            ingredient.setAmount(value);
        }
    }

    private static void initialiseOutputDirectories(String baseDir, String type) throws IOException {
        recursiveDelete(getSuccessPath(baseDir, type));
        Files.createDirectories(getSuccessPath(baseDir, type));
        recursiveDelete(getFailPath(baseDir, type));
        Files.createDirectories(getFailPath(baseDir, type));
    }

    public static void recursiveDelete(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

            });
        }
    }

    private static Path getSuccessPath(String baseDir, String type) {
        return FileSystems.getDefault().getPath(baseDir, type);
    }

    private static Path getFailPath(String baseDir, String type) {
        return FileSystems.getDefault().getPath(baseDir, type, "fail");
    }

}
