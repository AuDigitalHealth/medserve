package amt2fhir;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import amt2fhir.cache.ConceptCache;
import amt2fhir.enumeration.AmtConcept;
import amt2fhir.enumeration.AttributeType;
import amt2fhir.model.Concept;
import amt2fhir.model.DataTypeProperty;
import amt2fhir.model.Relationship;
import amt2fhir.util.FIleUtils;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.BoundCodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.QuantityDt;
import ca.uhn.fhir.model.dstu2.composite.RatioDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.dstu2.resource.Medication;
import ca.uhn.fhir.model.dstu2.resource.Medication.Package;
import ca.uhn.fhir.model.dstu2.resource.Medication.PackageContent;
import ca.uhn.fhir.model.dstu2.resource.Medication.Product;
import ca.uhn.fhir.model.dstu2.resource.Medication.ProductIngredient;
import ca.uhn.fhir.model.dstu2.resource.Substance;
import ca.uhn.fhir.model.dstu2.valueset.MedicationKindEnum;
import ca.uhn.fhir.model.dstu2.valueset.SubstanceTypeEnum;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;

public class Amt2Fhir {

    private static final Logger logger = Logger.getLogger(ConceptCache.class.getCanonicalName());

    private static IParser parser;
    private static FhirValidator validator;

	private static ConceptCache conceptCache;

    public static void main(String args[]) throws IOException, URISyntaxException {
		writeResourcesToFiles(FileSystems.getDefault().getPath(args[0]),
				FileSystems.getDefault().getPath(args[1]));
        
    }

	private static void writeResourcesToFiles(Path amtReleaseZipPath, Path outputPath) throws IOException {
		conceptCache = new ConceptCache(FileSystems.newFileSystem(
            URI.create("jar:file:" + amtReleaseZipPath.toAbsolutePath().toString()),
            new HashMap<>()));

        FIleUtils.initialiseOutputDirectories(outputPath, "mp");
        FIleUtils.initialiseOutputDirectories(outputPath, "mpuu");
        FIleUtils.initialiseOutputDirectories(outputPath, "mpp");
        FIleUtils.initialiseOutputDirectories(outputPath, "tpuu");
        FIleUtils.initialiseOutputDirectories(outputPath, "tpp");
        FIleUtils.initialiseOutputDirectories(outputPath, "ctpp");
        FIleUtils.initialiseOutputDirectories(outputPath, "substance");

        logger.info("initialised paths");
        
        parser = FhirContext.forDstu2().newJsonParser();
        parser.setPrettyPrint(true);
        validator = FhirContext.forDstu2().newValidator();

        conceptCache.getMps().values().forEach(concept -> writeResource(createProductResource(concept), outputPath, "mp"));
        logger.info("written MPs");
        conceptCache.getMpuus().values().forEach(concept -> writeResource(createProductResource(concept), outputPath, "mpuu"));
        logger.info("written MPUUs");
        conceptCache.getTpuus().values().forEach(concept -> writeResource(createProductResource(concept), outputPath, "tpuu"));
        logger.info("written TPUUs");
        conceptCache.getMpps().values().forEach(concept -> writeResource(createPackageResource(concept), outputPath, "mpp"));
        logger.info("written MPPs");
        conceptCache.getTpps().values().forEach(concept -> writeResource(createPackageResource(concept), outputPath, "tpp"));
        logger.info("written TPPs");
        conceptCache.getCtpps().values().forEach(concept -> writeResource(createPackageResource(concept), outputPath, "ctpp"));
        logger.info("written CTPPs");
        conceptCache.getSubstances().values().forEach(concept -> writeResource(createSubstanceResource(concept), outputPath, "substance"));
        logger.info("written Substances");
	}

	private static void writeResource(BaseResource resource, Path basePath, String type) {
        ValidationResult result = validator.validateWithResult(resource);

        try {
            if (result.isSuccessful()) {
                Files.write(FIleUtils.getSuccessPath(basePath, type).resolve(getFileName(resource)),
                    parser.encodeResourceToString(resource).getBytes(), StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            } else {
                Files.write(FIleUtils.getFailPath(basePath, type).resolve(getFileName(resource)),
                    parser.encodeResourceToString(resource).getBytes(), StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            }
        } catch (DataFormatException | IOException e) {
            logger.log(Level.SEVERE, "Failed writing out resource " + parser.encodeResourceToString(resource), e);
        }
    }

    private static String getFileName(BaseResource resource) {
    	String name;
    	String code;
    	if (resource instanceof Medication) {
			Medication medication = (Medication) resource;
			name = medication.getName().replaceAll("/", "_");
			code = medication.getCode().getCodingFirstRep().getCode();
		} else if (resource instanceof Substance) {
			Substance substance = (Substance) resource;
			name = substance.getType().getCodingFirstRep().getDisplay().replaceAll("/", "_");
			code = substance.getType().getCodingFirstRep().getCode();
		} else {
			name = resource.getResourceName() + "-" + resource.getId().toString();
			code = resource.getId().toString();
		}

		if (name.length() > 255) {
			return code + " - " + name.substring(0, 240 - code.length());
		}
    	
        return name;
    }

	private static Substance createSubstanceResource(Concept concept) {
		Substance substance = new Substance();
		substance.setId(Long.toString(concept.getId()));
		
		BoundCodeableConceptDt<SubstanceTypeEnum> type = new BoundCodeableConceptDt<SubstanceTypeEnum>(
				SubstanceTypeEnum.VALUESET_BINDER);
		type.setCoding(Arrays.asList(concept.toCodingDt()));
		substance.setType(type);
		concept.getMultipleDestinations(AttributeType.IS_MODIFICATION_OF).forEach(m ->
				substance.addIngredient().setSubstance(toReference(m, "Substance")));
		
		return substance;
	}
	
	private static Medication createBaseMedicationResource(Concept concept) {
		Medication medication = new Medication();
		medication.setId(Long.toString(concept.getId()));
	
	    medication.setName(concept.getPreferredTerm());
	    medication.setCode(concept.toCodeableConceptDt());
	    medication.setIsBrand(concept.hasParent(AmtConcept.TPUU));
		return medication;
	}

	private static Medication createPackageResource(Concept concept) {
        Medication medication = createBaseMedicationResource(concept);
        medication.setKind(MedicationKindEnum.PACKAGE);
        ca.uhn.fhir.model.dstu2.resource.Medication.Package pkg = new ca.uhn.fhir.model.dstu2.resource.Medication.Package();
        medication.setPackage(pkg);
        
        Concept container = concept.getSingleDestination(AttributeType.HAS_CONTAINER_TYPE);
        if (container != null) {
        	pkg.setContainer(container.toCodeableConceptDt());
        }
        
        concept.getRelationships(AttributeType.HAS_MPUU).forEach(
                r -> addProductReference(pkg, r));
        concept.getRelationships(AttributeType.HAS_TPUU).forEach(
                r -> addProductReference(pkg, r));

        concept.getRelationships(AttributeType.HAS_COMPONENT_PACK).forEach(r -> addProductReference(pkg, r));
        concept.getRelationships(AttributeType.HAS_SUBPACK).forEach(r -> addProductReference(pkg, r));
        
        return medication;
	}
	
    private static Medication createProductResource(Concept concept) {
	    Medication medication = createBaseMedicationResource(concept);
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

	private static void addProductReference(Package pkg, Relationship relationship) {
    	PackageContent content = pkg.addContent();

        Concept destination;
		// This is a dirty hack because AMT doesn't restate the
		// HAS_COMPONENT_PACK and HAS_SUBPACK relationships from MPP on TPP. As
		// a result these relationships on TPPs are the inferred relationships
		// from MPP which target MPPs not TPPs. This code tries to back stitch
		// to the TPP which should be the target of a stated HAS_SUBPACK or
		// HAS_COMPONENT_PACK relationship, but the real solution is to state
		// these in AMT
        if ((relationship.getType().equals(AttributeType.HAS_SUBPACK)
        		|| relationship.getType().equals(AttributeType.HAS_COMPONENT_PACK))
        		&& relationship.getSource().hasParent(AmtConcept.TPP)) {
        	Collection<Long> ctpp = conceptCache.getDescendantOf(relationship.getSource().getId());
        	if (ctpp.size() != 1) {
				throw new RuntimeException("More than one ctpp found for "
						+ relationship.getSource().toReference()
						+ " ctpps were " + StringUtils.join(ctpp, "'"));
        	}
        	
			destination = conceptCache.getConcept(ctpp.iterator().next()).getParents()
					.values().stream().filter(m -> m.hasParent(AmtConcept.TPP))
					.findFirst().get();
        } else {
        	destination = relationship.getDestination();
    	}

        content.setItem(toReference(destination, "Medication"));
        
        QuantityDt quantity;
        
		if (relationship.getType().equals(AttributeType.HAS_COMPONENT_PACK)) {
			quantity = new QuantityDt(1);
		} else {
			DataTypeProperty datatypeProperty = relationship.getDatatypeProperty();

			quantity = new QuantityDt(Double.valueOf(datatypeProperty
					.getValue()));
			quantity.setCode(Long.toString(datatypeProperty.getUnit().getId()));
			quantity.setUnits(datatypeProperty.getUnit().getPreferredTerm());
			quantity.setSystem(Concept.SNOMED_CT_SYSTEM_URI);
		}

        content.setAmount(quantity);
	}

	private static void addIngredient(Product product, Collection<Relationship> relationships) {
        Relationship iai = relationships.stream()
            .filter(r -> r.getType().equals(AttributeType.HAS_INTENDED_ACTIVE_INGREDIENT))
            .findFirst()
            .get();
        ProductIngredient ingredient = product.addIngredient();
        
        ingredient.setItem(toReference(iai.getDestination(), "Substance"));
        
        Relationship hasBoss = relationships.stream()
            .filter(r -> r.getType().equals(AttributeType.HAS_AUSTRALIAN_BOSS))
            .findFirst()
            .orElse(null);
        if (hasBoss != null) {
            DataTypeProperty d = hasBoss.getDatatypeProperty();
            
            ExtensionDt boss = new ExtensionDt();
            boss.setValue(hasBoss.getDestination().toCodingDt()).setUrl("bossExtension");
			ingredient.addUndeclaredExtension(boss);
            
            Concept denominatorUnit = d.getUnit().getSingleDestination(AttributeType.HAS_DENOMINATOR_UNITS);
            Concept numeratorUnit = d.getUnit().getSingleDestination(AttributeType.HAS_NUMERATOR_UNITS);

            RatioDt value = new RatioDt();

            QuantityDt denominator = new QuantityDt(1L);

            if (denominatorUnit != null) {
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

	private static ResourceReferenceDt toReference(Concept concept, String resourceType) {
		ResourceReferenceDt substanceReference = 
				new ResourceReferenceDt(resourceType + "/" + concept.getId());
        substanceReference.setDisplay(concept.getPreferredTerm());
        return substanceReference;
	}

}
