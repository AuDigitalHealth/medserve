package amt2fhir;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.exceptions.FHIRException;
import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.Medication.MedicationPackageComponent;
import org.hl7.fhir.dstu3.model.Medication.MedicationPackageContentComponent;
import org.hl7.fhir.dstu3.model.Medication.MedicationProductComponent;
import org.hl7.fhir.dstu3.model.Medication.MedicationProductIngredientComponent;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Narrative.NarrativeStatus;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Ratio;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.Substance;

import amt2fhir.cache.ConceptCache;
import amt2fhir.enumeration.AmtConcept;
import amt2fhir.enumeration.AttributeType;
import amt2fhir.model.Concept;
import amt2fhir.model.DataTypeProperty;
import amt2fhir.model.Relationship;
import amt2fhir.util.FileUtils;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;

public class Amt2Fhir {

    private static final String TPP_EXTENSION_URL = "http://github.com/dionmcm/amtOnFhir/tree/master/amt2fhir/fhir/extension/tpp/0.1.0";

    private static final String INPUT_FILE_OPTION = "i";

	private static final String URL_OPTION = "url";

	private static final String OUTPUT_FILE_OPTION = "o";

	private static final Logger logger = Logger.getLogger(ConceptCache.class.getCanonicalName());

    private IParser parser;
    private FhirValidator validator;

	private ConceptCache conceptCache;

	private IGenericClient client;

    public static void main(String args[]) throws IOException, URISyntaxException {
    	Options options = new Options();
		
		options.addOption(Option.builder(INPUT_FILE_OPTION).longOpt("inputFile")
				.argName("AMT_ZIP_FILE_PATH").hasArg()
				.desc("Input AMT release ZIP file").required(true).build());
		options.addOption(Option.builder(URL_OPTION)
				.argName("FHIR_SERVER_BASE_URL").hasArg()
				.desc("FHIR server URL to post Medication Resources to "
						+ "- e.g. http://fhir-dev.healthintersections.com.au/open/").build());
		options.addOption(Option.builder(OUTPUT_FILE_OPTION).longOpt("outputDirectory")
				.argName("OUTPUT_DIR").hasArg()
				.desc("Output directory to write out Medication Resources as files").build());

    	CommandLineParser parser = new DefaultParser();
		try {
			CommandLine line = parser.parse(options, args);
			
			Amt2Fhir amt2Fhir = new Amt2Fhir(FileSystems.getDefault().getPath(line.getOptionValue(INPUT_FILE_OPTION)));
			
			if (!line.hasOption(OUTPUT_FILE_OPTION) && !line.hasOption(URL_OPTION)) {
				throw new ParseException("At least one output mode -o or -url must be specified");
			}
			
			if (line.hasOption(URL_OPTION)) {
				amt2Fhir.postToFhirServer(line.getOptionValue(URL_OPTION));
			}
			
			if (line.hasOption(OUTPUT_FILE_OPTION)) {
				amt2Fhir.writeResourcesToFiles(FileSystems.getDefault().getPath(line.getOptionValue(OUTPUT_FILE_OPTION)));
			}
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Amt2Fhir", options);
		}
    }
    
    public Amt2Fhir(Path amtReleaseZipPath) throws IOException {
		conceptCache = new ConceptCache(FileSystems.newFileSystem(
	            URI.create("jar:file:" + amtReleaseZipPath.toAbsolutePath().toString()),
	            new HashMap<>()));
    }
    
    public void postToFhirServer(String url) {
		client = FhirContext.forDstu2().newRestfulGenericClient(url);
		
        process(getCreateResourceConsumer(url));
    }

	public void writeResourcesToFiles(Path outputPath) throws IOException {
        parser = FhirContext.forDstu3().newJsonParser();
        parser.setPrettyPrint(true);
        validator = FhirContext.forDstu3().newValidator();
        
        FileUtils.initialiseOutputDirectories(outputPath, "mp");
        FileUtils.initialiseOutputDirectories(outputPath, "mpuu");
        FileUtils.initialiseOutputDirectories(outputPath, "mpp");
        FileUtils.initialiseOutputDirectories(outputPath, "tpuu");
        FileUtils.initialiseOutputDirectories(outputPath, "tpp");
        FileUtils.initialiseOutputDirectories(outputPath, "ctpp");
        FileUtils.initialiseOutputDirectories(outputPath, "substance");

        logger.info("initialised paths");

        process(getWriteResourceConsumer(outputPath));
	}
	
	private BiConsumer<BaseResource, String> getWriteResourceConsumer(Path outputPath) {
		return (resource, type) -> writeResource(resource, outputPath, type);
	}
	
	private BiConsumer<BaseResource, String> getCreateResourceConsumer(String url) {
		return (resource, type) -> createResourceOnServer(resource, url, type);
	}

	private void createResourceOnServer(BaseResource resource, String url, String string) {
		MethodOutcome outcome = client.update(resource.getId(), resource);
		// TODO - do some validation of the outcome
	}

    private void process(BiConsumer<BaseResource, String> consumer) {
		conceptCache.getMps().values().stream()
				.map(concept -> createProductResource(concept))
				.forEach(resource -> consumer.accept(resource, "mp"));
		logger.info("written MPs");
		conceptCache.getMpuus().values().stream()
				.map(concept -> createProductResource(concept))
				.forEach(resource -> consumer.accept(resource, "mpuu"));
		logger.info("written MPUUs");
		conceptCache.getTpuus().values().stream()
				.map(concept -> createProductResource(concept))
				.forEach(resource -> consumer.accept(resource, "tpuu"));
		logger.info("written TPUUs");
		conceptCache.getMpps().values().stream()
				.map(concept -> createPackageResource(concept))
				.forEach(resource -> consumer.accept(resource, "mpp"));
		logger.info("written MPPs");
		conceptCache.getTpps().values().stream()
				.map(concept -> createPackageResource(concept))
				.forEach(resource -> consumer.accept(resource, "tpp"));
		logger.info("written TPPs");
		conceptCache.getCtpps().values().stream()
				.map(concept -> createPackageResource(concept))
				.forEach(resource -> consumer.accept(resource, "ctpp"));
		logger.info("written CTPPs");
		conceptCache.getSubstances().values().stream()
				.map(concept -> createSubstanceResource(concept))
				.forEach(resource -> consumer.accept(resource, "substance"));
		logger.info("written Substances");
	}

	private void writeResource(BaseResource resource, Path basePath, String type) {
        ValidationResult result = validator.validateWithResult(resource);

        try {
            if (result.isSuccessful()) {
                Files.write(FileUtils.getSuccessPath(basePath, type).resolve(getFileName(resource)),
                    parser.encodeResourceToString(resource).getBytes(), StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            } else {
                Files.write(FileUtils.getFailPath(basePath, type).resolve(getFileName(resource)),
                    parser.encodeResourceToString(resource).getBytes(), StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
                Files.write(FileUtils.getFailPath(basePath, type).resolve(getFileName(resource)),
                    result.toString().getBytes(), StandardOpenOption.APPEND, StandardOpenOption.WRITE);
            }
        } catch (DataFormatException | IOException e) {
            logger.log(Level.SEVERE, "Failed writing out resource " + parser.encodeResourceToString(resource), e);
        }
    }

    private String getFileName(BaseResource resource) {
        String name;
    	String code;
    	if (resource instanceof Medication) {
			Medication medication = (Medication) resource;
            name = medication.getCode().getCodingFirstRep().getDisplay().replaceAll("/", "_").replaceAll("'", "");
			code = medication.getCode().getCodingFirstRep().getCode();
		} else if (resource instanceof Substance) {
			Substance substance = (Substance) resource;
            name = substance.getCode().getCodingFirstRep().getDisplay().replaceAll("/", "_").replaceAll("'", "");
            code = substance.getCode().getCodingFirstRep().getCode();
		} else {
            throw new RuntimeException("Unknown resource type " + resource.getClass().getName());
		}

        if (name.length() > 200) {
            return name.substring(0, 200 - code.length()) + "_" + code;
		}
    	
        return name + "_" + code;
    }

	private Substance createSubstanceResource(Concept concept) {
		Substance substance = new Substance();
		setStandardResourceElements(concept, substance);
		
        substance.setCode(concept.toCodeableConcept());
		concept.getMultipleDestinations(AttributeType.IS_MODIFICATION_OF).forEach(m ->
				substance.addIngredient().setSubstance(toReference(m, "Substance")));
		
		return substance;
	}
	
	private Medication createBaseMedicationResource(Concept concept) {
		Medication medication = new Medication();
		setStandardResourceElements(concept, medication);

        // medication.setName(concept.getPreferredTerm());
	    medication.setCode(concept.toCodeableConcept());
        medication.setIsBrand(concept.hasParent(AmtConcept.TPUU) || concept.hasParent(AmtConcept.TPP)
                || concept.hasParent(AmtConcept.CTPP));

        Set<String> artgIds = conceptCache.getArtgId(concept.getId());
        if (artgIds != null) {
            for (String id : artgIds) {
                Coding codingDt = medication.getCode().addCoding();
                codingDt.setSystem("https://www.tga.gov.au/australian-register-therapeutic-goods");
                codingDt.setCode(id);
            }
        }

		return medication;
	}

    private void setStandardResourceElements(Concept concept, DomainResource resource) {
		resource.setId(Long.toString(concept.getId()));
        Narrative narrative = new Narrative();
        narrative.setStatus(NarrativeStatus.GENERATED);
        narrative.setDivAsString("<div><p>" + StringEscapeUtils.escapeHtml3(concept.getPreferredTerm()) + "</p></div>");
		resource.setText(narrative);
	}

    private Medication createPackageResource(Concept concept) {
        Medication medication = createBaseMedicationResource(concept);
        MedicationPackageComponent pkg = new MedicationPackageComponent();
        medication.setPackage(pkg);
        
        Concept container = concept.getSingleDestination(AttributeType.HAS_CONTAINER_TYPE);
        if (container != null) {
        	pkg.setContainer(container.toCodeableConcept());
        }
        
        concept.getRelationships(AttributeType.HAS_MPUU).forEach(r -> addProductReference(pkg, r));
        concept.getRelationships(AttributeType.HAS_TPUU).forEach(r -> addProductReference(pkg, r));

        concept.getRelationships(AttributeType.HAS_COMPONENT_PACK).forEach(r -> addProductReference(pkg, r));
        concept.getRelationships(AttributeType.HAS_SUBPACK).forEach(r -> addProductReference(pkg, r));
        
        if (concept.hasParent(AmtConcept.CTPP)) {
            List<Concept> tpps = concept.getParents()
                .values()
                .stream()
                .filter(c -> c.getId() != AmtConcept.CTPP.getId())
                .collect(Collectors.toList());
            if (tpps.size() != 1) {
                throw new RuntimeException(
                    "Expect only one tpp for ctpp " + concept.getId() + " but found " + tpps.size());
            }
            try {
                addTppExtension(tpps.iterator().next(), medication);
            } catch (FHIRException e) {
                throw new RuntimeException(e);
            }
        }

        return medication;
	}
	
    private void addTppExtension(Concept tpp, Medication medication) throws FHIRException {
        Extension extension = medication.addExtension();
        extension.setUrl(TPP_EXTENSION_URL);
        extension.setValue(tpp.toCoding());
    }

    private Medication createProductResource(Concept concept) {
	    Medication medication = createBaseMedicationResource(concept);
        MedicationProductComponent product = new MedicationProductComponent();
	    medication.setProduct(product);
	    concept.getRelationshipGroupsContaining(AttributeType.HAS_INTENDED_ACTIVE_INGREDIENT).forEach(
	        r -> addIngredient(product, r));
	
	    Concept form = concept.getSingleDestination(AttributeType.HAS_MANUFACTURED_DOSE_FORM);
	    if (form != null) {
	        product.setForm(form.toCodeableConcept());
	    }
	    return medication;
	}

    private void addProductReference(MedicationPackageComponent pkg, Relationship relationship) {
        MedicationPackageContentComponent content = pkg.addContent();

        Concept destination = null;
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
            
            Set<Concept> destinationSet = conceptCache.getConcept(ctpp.iterator().next())
                .getRelationships(relationship.getType())
                .stream()
                .flatMap(r -> r.getDestination().getParents().values().stream())
                .filter(c -> c.hasParent(relationship.getDestination()))
                .collect(Collectors.toSet());

            if (destinationSet.size() != 1) {
                throw new RuntimeException("Destination set was expected to be 1 but was " + destinationSet);
            }

            destination = destinationSet.iterator().next();

        } else {
        	destination = relationship.getDestination();
    	}

        content.setItem(toReference(destination, "Medication"));
        
        SimpleQuantity quantity;
        
		if (relationship.getType().equals(AttributeType.HAS_COMPONENT_PACK)) {
            quantity = new SimpleQuantity();
            quantity.setValue(1);
		} else {
			DataTypeProperty datatypeProperty = relationship.getDatatypeProperty();

            quantity = new SimpleQuantity();
            quantity.setValue(Double.valueOf(datatypeProperty.getValue()));
			quantity.setCode(Long.toString(datatypeProperty.getUnit().getId()));
            quantity.setUnit(datatypeProperty.getUnit().getPreferredTerm());
			quantity.setSystem(Concept.SNOMED_CT_SYSTEM_URI);
		}

        content.setAmount(quantity);
	}

    private void addIngredient(MedicationProductComponent product, Collection<Relationship> relationships) {
        Relationship iai = relationships.stream()
            .filter(r -> r.getType().equals(AttributeType.HAS_INTENDED_ACTIVE_INGREDIENT))
            .findFirst()
            .get();

        MedicationProductIngredientComponent ingredient = product.addIngredient();
        
        ingredient.setItem(toReference(iai.getDestination(), "Substance"));
        
        Relationship hasBoss = relationships.stream()
            .filter(r -> r.getType().equals(AttributeType.HAS_AUSTRALIAN_BOSS))
            .findFirst()
            .orElse(null);
        if (hasBoss != null) {
            DataTypeProperty d = hasBoss.getDatatypeProperty();
            
            Extension boss = new Extension();
            boss.setValue(hasBoss.getDestination().toCoding()).setUrl("bossExtension");
            ingredient.addExtension(boss);
            
            Concept denominatorUnit = d.getUnit().getSingleDestination(AttributeType.HAS_DENOMINATOR_UNITS);
            Concept numeratorUnit = d.getUnit().getSingleDestination(AttributeType.HAS_NUMERATOR_UNITS);

            Ratio value = new Ratio();

            Quantity denominator = new Quantity(1L);

            if (denominatorUnit != null) {
                denominator.setCode(Long.toString(denominatorUnit.getId()));
                denominator.setUnit(denominatorUnit.getPreferredTerm());
                denominator.setSystem(Concept.SNOMED_CT_SYSTEM_URI);
            }
            value.setDenominator(denominator);

            Quantity numerator = new Quantity(Double.parseDouble(d.getValue()));
            numerator.setCode(Long.toString(numeratorUnit.getId()));
            numerator.setUnit(numeratorUnit.getPreferredTerm());
            numerator.setSystem(Concept.SNOMED_CT_SYSTEM_URI);
            value.setNumerator(numerator);

            ingredient.setAmount(value);
        }
    }

    private Reference toReference(Concept concept, String resourceType) {
        Reference substanceReference =
                new Reference(resourceType + "/" + concept.getId());
        substanceReference.setDisplay(concept.getPreferredTerm());
        return substanceReference;
    }

}
