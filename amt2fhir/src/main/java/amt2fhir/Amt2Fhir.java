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

import amt2fhir.cache.ConceptCache;
import amt2fhir.enumeration.AmtConcept;
import amt2fhir.enumeration.AttributeType;
import amt2fhir.model.Concept;
import amt2fhir.model.DataTypeProperty;
import amt2fhir.model.Relationship;
import amt2fhir.util.FileUtils;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.BoundCodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.NarrativeDt;
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
import ca.uhn.fhir.model.dstu2.valueset.NarrativeStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.SubstanceTypeEnum;
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
        parser = FhirContext.forDstu2().newJsonParser();
        parser.setPrettyPrint(true);
        validator = FhirContext.forDstu2().newValidator();
        
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

        if (name.length() > 200) {
            return name.substring(0, 200 - code.length()) + "_" + code;
		}
    	
        return name + "_" + code;
    }

	private Substance createSubstanceResource(Concept concept) {
		Substance substance = new Substance();
		setStandardResourceElements(concept, substance);
		
		BoundCodeableConceptDt<SubstanceTypeEnum> type = new BoundCodeableConceptDt<SubstanceTypeEnum>(
				SubstanceTypeEnum.VALUESET_BINDER);
		type.setCoding(Arrays.asList(concept.toCodingDt()));
		substance.setType(type);
		concept.getMultipleDestinations(AttributeType.IS_MODIFICATION_OF).forEach(m ->
				substance.addIngredient().setSubstance(toReference(m, "Substance")));
		
		return substance;
	}
	
	private Medication createBaseMedicationResource(Concept concept) {
		Medication medication = new Medication();
		setStandardResourceElements(concept, medication);

	    medication.setName(concept.getPreferredTerm());
	    medication.setCode(concept.toCodeableConceptDt());
	    medication.setIsBrand(concept.hasParent(AmtConcept.TPUU));

        Set<String> artgIds = conceptCache.getArtgId(concept.getId());
        if (artgIds != null) {
            for (String id : artgIds) {
                CodingDt codingDt = medication.getCode().addCoding();
                codingDt.setSystem("https://www.tga.gov.au/australian-register-therapeutic-goods");
                codingDt.setCode(id);
            }
        }

		return medication;
	}

	private void setStandardResourceElements(Concept concept,
			BaseResource resource) {
		resource.setId(Long.toString(concept.getId()));
		NarrativeDt narrative = new NarrativeDt();
		narrative.setStatus(NarrativeStatusEnum.GENERATED);
		narrative.setDiv("<div><p>" + StringEscapeUtils.escapeHtml3(concept.getPreferredTerm()) + "</p></div>");
		resource.setText(narrative);
	}

	private Medication createPackageResource(Concept concept) {
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
            addTppExtension(tpps.iterator().next(), medication);
        }

        return medication;
	}
	
    private void addTppExtension(Concept tpp, Medication medication) {
        medication.addUndeclaredExtension(false,
            TPP_EXTENSION_URL,
            tpp.toCodingDt());
    }

    private Medication createProductResource(Concept concept) {
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

	private void addProductReference(Package pkg, Relationship relationship) {
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

	private void addIngredient(Product product, Collection<Relationship> relationships) {
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

	private ResourceReferenceDt toReference(Concept concept, String resourceType) {
		ResourceReferenceDt substanceReference = 
				new ResourceReferenceDt(resourceType + "/" + concept.getId());
        substanceReference.setDisplay(concept.getPreferredTerm());
        return substanceReference;
	}

}
