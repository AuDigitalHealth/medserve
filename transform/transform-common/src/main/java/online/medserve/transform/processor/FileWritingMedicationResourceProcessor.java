package online.medserve.transform.processor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.Substance;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import online.medserve.extension.ExtendedMedication;
import online.medserve.extension.MedicationType;
import online.medserve.transform.util.FhirCodeSystemUri;
import online.medserve.transform.util.FileUtils;

public class FileWritingMedicationResourceProcessor implements MedicationResourceProcessor {

    private static final Logger logger =
            Logger.getLogger(FileWritingMedicationResourceProcessor.class.getCanonicalName());
    private IParser parser;
    private FhirValidator validator;
    private Path outputPath;
    private Set<String> types = new HashSet<>();
    private Collection<String> baseSystems =
            Arrays.asList(FhirCodeSystemUri.SNOMED_CT_SYSTEM_URI.getUri(), FhirCodeSystemUri.RXNORM_URI.getUri());

    public FileWritingMedicationResourceProcessor(Path outputPath) throws IOException {
        this.outputPath = outputPath;

        parser = FhirContext.forDstu3().newJsonParser();
        parser.setPrettyPrint(true);
        validator = FhirContext.forDstu3().newValidator();
    }

    @Override
    public void processResources(List<? extends Resource> resources) throws IOException {
        for (Resource resource : resources) {
            writeResource(resource, outputPath);
        }
    }

    private void writeResource(BaseResource resource, Path basePath) throws IOException {
        String type = determineType(resource);

        if (!types.contains(type)) {
            types.add(type);
            FileUtils.initialiseOutputDirectories(outputPath, type);
        }

        ValidationResult result = validator.validateWithResult(resource);

        try {
            String fileName = getFileName(resource);
            if (result.isSuccessful()) {
                Files.write(FileUtils.getSuccessPath(basePath, type).resolve(fileName),
                    parser.encodeResourceToString(resource).getBytes(), StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            } else {
                Files.write(FileUtils.getFailPath(basePath, type).resolve(fileName),
                    parser.encodeResourceToString(resource).getBytes(), StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
                Files.write(FileUtils.getFailPath(basePath, type).resolve(fileName),
                    result.toString().getBytes(), StandardOpenOption.APPEND, StandardOpenOption.WRITE);
                logger.warning("Resource " + fileName + " is not valid!!!");
            }
        } catch (DataFormatException | IOException e) {
            logger.log(Level.SEVERE, "Failed writing out resource " + parser.encodeResourceToString(resource), e);
        }
    }

    private String determineType(BaseResource resource) {
        String type;
        if (ExtendedMedication.class.isAssignableFrom(resource.getClass())) {
            String code = ExtendedMedication.class.cast(resource).getMedicationResourceType().getCode();
            type = MedicationType.fromCode(code).name();
        } else {
            type = resource.fhirType();
        }

        return type;
    }

    private String getFileName(BaseResource resource) {
        String name;
        String code;
        if (resource instanceof Medication) {
            Medication medication = (Medication) resource;
            Coding coding = medication.getCode()
                .getCoding()
                .stream()
                .filter(c -> baseSystems.contains(c.getSystem()))
                .findFirst()
                .get();
            name = coding.getDisplay().replaceAll("/", "_").replaceAll("'", "");
            code = coding.getCode();
        } else if (resource instanceof Substance) {
            Substance substance = (Substance) resource;
            Coding coding = substance.getCode()
                .getCoding()
                .stream()
                .filter(c -> baseSystems.contains(c.getSystem()))
                .findFirst()
                .get();
            name = coding.getDisplay().replaceAll("/", "_").replaceAll("'", "");
            code = coding.getCode();
        } else if (resource instanceof Organization) {
            Organization org = (Organization) resource;
            name = org.getName().replaceAll("/", "_").replaceAll("'", "");
            code = org.getId();
        } else {
            throw new RuntimeException("Unknown resource type " + resource.getClass().getName());
        }

        if (name.length() > 200) {
            return name.substring(0, 200 - code.length()) + "_" + code;
        }

        return name + "_" + code;
    }
}
