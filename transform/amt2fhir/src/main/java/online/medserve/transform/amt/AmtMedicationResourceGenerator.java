package online.medserve.transform.amt;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.DecimalType;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Enumeration;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.Medication.MedicationPackageComponent;
import org.hl7.fhir.dstu3.model.Medication.MedicationPackageContentComponent;
import org.hl7.fhir.dstu3.model.Medication.MedicationStatus;
import org.hl7.fhir.dstu3.model.Medication.MedicationStatusEnumFactory;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Narrative.NarrativeStatus;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Ratio;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Substance.FHIRSubstanceStatus;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.dstu3.model.codesystems.SubstanceStatus;
import org.hl7.fhir.dstu3.model.codesystems.SubstanceStatusEnumFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import online.medserve.FhirCodeSystemUri;
import online.medserve.extension.ElementWithHistoricalMedicationReferences;
import online.medserve.extension.ElementWithHistoricalSubstanceReferences;
import online.medserve.extension.ExtendedMedication;
import online.medserve.extension.ExtendedMedicationReference;
import online.medserve.extension.ExtendedSubstance;
import online.medserve.extension.ExtendedSubstanceReference;
import online.medserve.extension.MedicationIngredientComponentExtension;
import online.medserve.extension.ParentExtendedElement;
import online.medserve.extension.SourceCodeSystemExtension;
import online.medserve.extension.SubsidyExtension;
import online.medserve.transform.amt.cache.AmtCache;
import online.medserve.transform.amt.cache.PbsCodeSystemUtil;
import online.medserve.transform.amt.enumeration.AmtConcept;
import online.medserve.transform.amt.enumeration.AttributeType;
import online.medserve.transform.amt.model.Concept;
import online.medserve.transform.amt.model.ConceptReplacement;
import online.medserve.transform.amt.model.DataTypeProperty;
import online.medserve.transform.amt.model.Manufacturer;
import online.medserve.transform.amt.model.Relationship;
import online.medserve.transform.amt.model.Subsidy;
import online.medserve.transform.processor.MedicationResourceProcessor;
import online.medserve.transform.util.FileUtils;

public class AmtMedicationResourceGenerator {
    private static final Logger logger = Logger.getLogger(AmtMedicationResourceGenerator.class.getCanonicalName());

    private static final String AMT_FILE_PATTERN =
            "NCTS_SCT_RF2_DISTRIBUTION_32506021000036107-(\\d{8})-SNAPSHOT\\.zip";
    private static Pattern amtFilePattern = Pattern.compile(AMT_FILE_PATTERN);

    private AmtCache conceptCache;
    private Set<String> processedConcepts = new HashSet<>();
    private String amtVersion;

    FhirValidator validator = FhirContext.forDstu3().newValidator();

    private Map<Long, ExtendedSubstanceReference> extendedSubstanceReferenceCache = new HashMap<>();

    private Map<Long, ExtendedMedicationReference> extendedMedicationReferenceCache = new HashMap<>();

    public AmtMedicationResourceGenerator(Path amtReleaseZipPath, Path pbsExtractPath)
            throws IOException {
        Matcher amtFileNameMatcher = amtFilePattern.matcher(amtReleaseZipPath.getFileName().toString());
        if (!amtFileNameMatcher.matches()) {
            throw new IllegalArgumentException("AMT file name " + amtReleaseZipPath.getFileName()
                    + " does not match expected pattern " + AMT_FILE_PATTERN);
        }

        amtVersion = "http://snomed.info/sct?version=http%3A%2F%2Fsnomed.info%2Fsct%2F32506021000036107%2Fversion%2F"
                + amtFileNameMatcher.group(1);

        this.conceptCache =
                new AmtCache(FileUtils.getFileSystemForZipPath(amtReleaseZipPath),
                    FileUtils.getFileSystemForZipPath(pbsExtractPath));
    }

    public void process(MedicationResourceProcessor processor) throws IOException {
        processConceptList(conceptCache.getCtpps(), "CTPP", processor);

        logger.info("Mopping up concepts unreferenced by CTPPs");

        processConceptList(conceptCache.getTpps(), "TPP", processor);
        processConceptList(conceptCache.getMpps(), "MPP", processor);
        processConceptList(conceptCache.getTpuus(), "TPUU", processor);
        processConceptList(conceptCache.getMpuus(), "MPUU", processor);
        processConceptList(conceptCache.getMps(), "MP", processor);
        processConceptList(conceptCache.getSubstances(), "Substance", processor);

        logger.info("Finished creating " + processedConcepts.size() + " resources");
    }

    private void processConceptList(Map<Long, Concept> conceptList, String conceptType,
            MedicationResourceProcessor processor)
            throws IOException {
        logger.info("Processing " + conceptList.size() + " " + conceptType + " concepts");

        List<Resource> createdResources = new ArrayList<>();
        int processedConceptsStartingSize = processedConcepts.size();
        int counter = 0;
        for (Concept concept : conceptCache.getCtpps().values()) {
            counter++;
            createdResources.clear();
            if (!processedConcepts.contains(Long.toString(concept.getId()))) {
                createPackageResource(concept, createdResources);
                processor.processResources(createdResources);
            }
            if (counter % 1000 == 0) {
                logger.info("Processed " + counter + " " + conceptType + "s...");
            }
        }

        logger.info("Completed processing " + conceptList.size() + " " + conceptType + "s, added "
                + (processedConcepts.size() - processedConceptsStartingSize) + " resources");
    }

    private ExtendedSubstanceReference createSubstanceResource(Concept concept, List<Resource> createdResources) {
        ExtendedSubstanceReference reference =
                getExtendedSubstanceReference(concept, new HashSet<>(), createdResources);
        if (!processedConcepts.contains(Long.toString(concept.getId()))) {
            processedConcepts.add(Long.toString(concept.getId()));
            ExtendedSubstance substance = new ExtendedSubstance();
            setStandardResourceElements(concept, substance);
            substance.setSourceCodeSystem(
                new SourceCodeSystemExtension(new UriType(FhirCodeSystemUri.SNOMED_CT_SYSTEM_URI.getUri()),
                    new StringType(amtVersion)));
            addHistoicalAssociations(concept, substance, new HashSet<>(), createdResources);
            substance.setCode(concept.toCodeableConcept());
            substance.setStatus(concept.isActive() ? FHIRSubstanceStatus.ACTIVE : FHIRSubstanceStatus.ENTEREDINERROR);
            substance.setLastModified(new DateType(concept.getLastModified()));

            concept.getMultipleDestinations(AttributeType.IS_MODIFICATION_OF)
                .forEach(m -> substance.addIngredient().setSubstance(createSubstanceResource(m, createdResources)));
            createdResources.add(substance);
        }

        return reference;
    }

    private ExtendedMedication createBaseMedicationResource(Concept concept, List<Resource> createdResources) {
        ExtendedMedication medication = new ExtendedMedication();
        medication.setSourceCodeSystem(
            new SourceCodeSystemExtension(new UriType(FhirCodeSystemUri.SNOMED_CT_SYSTEM_URI.getUri()),
                new StringType(amtVersion)));
        setStandardResourceElements(concept, medication);

        addHistoicalAssociations(concept, medication, new HashSet<>(), createdResources);

        medication.setLastModified(new DateType(concept.getLastModified()));

        medication.setCode(concept.toCodeableConcept());

        medication.setStatus(concept.getMedicationStatus());

        medication.setMedicationResourceType(concept.getMedicationType().getCode());

        addParentExtensions(concept, medication, new HashSet<>(), createdResources);

        medication.setIsBrand(concept.getMedicationType().isBranded());

        Set<String> artgIds = conceptCache.getArtgId(concept.getId());
        if (artgIds != null) {
            for (String id : artgIds) {
                Coding codingDt = medication.getCode().addCoding();
                codingDt.setSystem(FhirCodeSystemUri.TGA_URI.getUri());
                codingDt.setCode(id);
            }
        }

        if (concept.hasAtLeastOneMatchingAncestor(AmtConcept.TP)) {
            if (concept.getAncestors(AmtConcept.TP)
                .stream()
                .filter(c -> !AmtConcept.isEnumValue(c.getId()))
                .count() > 1) {
                throw new RuntimeException("more than one TP " + concept.getAncestors(AmtConcept.TP));
            }
            Concept brand = concept.getAncestors(AmtConcept.TP)
                .stream()
                .filter(c -> !AmtConcept.isEnumValue(c.getId()))
                .iterator()
                .next();
            medication.setBrand(brand.toCodeableConcept());
        } else if (concept.hasAtLeastOneMatchingAncestor(AmtConcept.TPP)) {
            Concept brand = concept.getSingleDestination(AttributeType.HAS_TP);
            medication.setBrand(brand.toCodeableConcept());
        }

        return medication;
    }

    private void addHistoicalAssociations(Concept concept, ElementWithHistoricalMedicationReferences resource,
            Set<Long> addedConcepts,
            List<Resource> createdResources) {
        if (concept.getReplacementConcept() != null) {
            for (ConceptReplacement replacement : concept.getReplacementConcept()) {
                if (replacement.getConcept().hasAtLeastOneMatchingAncestor(AmtConcept.SUBSTANCE)) {
                    logger.warning("Replacement " + replacement.getConcept() + " for concept " + concept
                            + " is not of the right type");
                    continue;
                }
                ExtendedMedicationReference reference =
                        toExtendedMedicationReference(replacement.getConcept(), addedConcepts, createdResources);

                reference.setReplacementDate(new DateType(replacement.getReplacementDate()));
                reference.setReplacementType(AmtConcept.fromId(replacement.getType()).toCoding());
                reference.setReplacesHistoricallyRelatedResource(new BooleanType(false));
                resource.getIsReplacedByResources().add(reference);
            }
        }

        if (concept.getReplacedConcept() != null) {
            for (ConceptReplacement replaced : concept.getReplacedConcept()) {

                if (replaced.getConcept().hasAtLeastOneMatchingAncestor(AmtConcept.SUBSTANCE)) {
                    logger.warning("Replacement " + concept + " for concept " + replaced.getConcept()
                            + " is not of the right type");
                    continue;
                }
                ExtendedMedicationReference reference =
                        toExtendedMedicationReference(replaced.getConcept(), addedConcepts, createdResources);

                reference.setReplacementDate(new DateType(replaced.getReplacementDate()));
                reference.setReplacementType(AmtConcept.fromId(replaced.getType()).toCoding());
                reference.setReplacesHistoricallyRelatedResource(new BooleanType(true));
                resource.getReplacesResources().add(reference);
            }
        }
    }

    private void addHistoicalAssociations(Concept concept, ElementWithHistoricalSubstanceReferences resource,
            Set<Long> addedConcepts,
            List<Resource> createdResources) {
        if (concept.getReplacementConcept() != null) {
            for (ConceptReplacement replacement : concept.getReplacementConcept()) {
                if (replacement.getConcept().hasAtLeastOneMatchingAncestor(AmtConcept.MP)
                        || replacement.getConcept().hasAtLeastOneMatchingAncestor(AmtConcept.MPP)) {
                    logger.warning("Replacement " + replacement.getConcept() + " for concept " + concept
                            + " is not of the right type");
                    continue;
                }
                ExtendedSubstanceReference reference =
                        toExtendedSubstanceReference(replacement.getConcept(), addedConcepts, createdResources);

                reference.setReplacementDate(new DateType(replacement.getReplacementDate()));
                reference.setReplacementType(AmtConcept.fromId(replacement.getType()).toCoding());
                reference.setReplacesHistoricallyRelatedResource(new BooleanType(false));
                resource.getIsReplacedByResources().add(reference);
            }
        }

        if (concept.getReplacedConcept() != null) {
            for (ConceptReplacement replaced : concept.getReplacedConcept()) {
                if (replaced.getConcept().hasAtLeastOneMatchingAncestor(AmtConcept.MP)
                        || replaced.getConcept().hasAtLeastOneMatchingAncestor(AmtConcept.MPP)) {
                    logger.warning("Replacement " + concept + " for concept " + replaced.getConcept()
                            + " is not of the right type");
                    continue;
                }
                ExtendedSubstanceReference reference =
                        toExtendedSubstanceReference(replaced.getConcept(), addedConcepts, createdResources);

                reference.setReplacementDate(new DateType(replaced.getReplacementDate()));
                reference.setReplacementType(AmtConcept.fromId(replaced.getType()).toCoding());
                reference.setReplacesHistoricallyRelatedResource(new BooleanType(true));
                resource.getReplacesResources().add(reference);
            }
        }
    }

    private void setStandardResourceElements(Concept concept, DomainResource resource) {
        resource.setId(Long.toString(concept.getId()));
        Narrative narrative = new Narrative();
        narrative.setStatus(NarrativeStatus.GENERATED);
        narrative.setDivAsString("<div><p>" + StringEscapeUtils.escapeHtml3(concept.getPreferredTerm()) + "</p></div>");
        resource.setText(narrative);
    }

    private void addParentExtensions(Concept concept, ParentExtendedElement element, Set<Long> addedConcepts,
            List<Resource> createdResources) {

        concept.getParents()
            .values()
            .stream()
            .filter(parent -> parent.isActive() || !concept.isActive())
            .filter(parent -> !AmtConcept.isEnumValue(Long.toString(parent.getId())))
            .filter(parent -> !addedConcepts.contains(parent.getId()))
            .forEach(parent -> {
                if (!parent.hasParent(AmtConcept.TP)) {

                    element.addParentMedicationResource(
                        getExtendedMedicationReference(parent, addedConcepts, createdResources));
                    addedConcepts.add(parent.getId());

                    switch (parent.getMedicationType()) {
                        case BrandedPackage:
                        case BrandedPackgeContainer:
                        case UnbrandedPackage:
                            createPackageResource(parent, createdResources);
                            break;

                        default:
                            createProductResource(parent, createdResources);
                            break;
                    }

                }
            });
    }

    private Reference createPackageResource(Concept concept, List<Resource> createdResources) {
        Reference reference = getExtendedMedicationReference(concept, new HashSet<>(), createdResources);
        if (!processedConcepts.contains(Long.toString(concept.getId()))) {
            processedConcepts.add(Long.toString(concept.getId()));
            ExtendedMedication medication = createBaseMedicationResource(concept, createdResources);
            MedicationPackageComponent pkg = new MedicationPackageComponent();
            medication.setPackage(pkg);

            Concept container = concept.getSingleDestination(AttributeType.HAS_CONTAINER_TYPE);
            if (container != null) {
                pkg.setContainer(container.toCodeableConcept());
            }

            concept.getRelationships(AttributeType.HAS_MPUU)
                .forEach(r -> addProductReference(pkg, r, createdResources));
            concept.getRelationships(AttributeType.HAS_TPUU)
                .forEach(r -> addProductReference(pkg, r, createdResources));

            concept.getRelationships(AttributeType.HAS_COMPONENT_PACK)
                .forEach(r -> addProductReference(pkg, r, createdResources));
            concept.getRelationships(AttributeType.HAS_SUBPACK)
                .forEach(r -> addProductReference(pkg, r, createdResources));

            concept.getSubsidies().forEach(subsidy -> addSubsidy(medication, subsidy));

            if (concept.hasParent(AmtConcept.CTPP)) {
                concept.getParents()
                    .values()
                    .stream()
                    .filter(c -> c.hasAtLeastOneMatchingAncestor(AmtConcept.TPP)
                            && !c.hasAtLeastOneMatchingAncestor(AmtConcept.CTPP))
                    .flatMap(c -> c.getSubsidies().stream())
                    .forEach(subsidy -> addSubsidy(medication, subsidy));
            }

            Manufacturer manufacturer = concept.getManufacturer();
            if (manufacturer != null) {
                medication.setManufacturer(createOrganisation(createdResources, manufacturer));
            }

            createdResources.add(medication);
        }
        return reference;
    }

    private Reference createOrganisation(List<Resource> createdResources, Manufacturer manufacturer) {
        Reference orgRef = new Reference("Organization/" + manufacturer.getCode());
        orgRef.setDisplay(manufacturer.getName());
        if (!processedConcepts.contains(manufacturer.getCode())) {
            processedConcepts.add(manufacturer.getCode());
            Organization org = new Organization();
            org.setId(manufacturer.getCode());
            Narrative narrative = new Narrative();
            narrative.setDivAsString(StringEscapeUtils.escapeHtml3(manufacturer.getName()));
            narrative.setStatusAsString("generated");
            org.setText(narrative);
            org.addAddress().addLine(manufacturer.getAddress());
            org.setName(manufacturer.getName());
            ContactPoint cp = org.addTelecom();
            cp.setSystem(ContactPoint.ContactPointSystem.PHONE);
            cp.setUse(ContactPoint.ContactPointUse.WORK);
            cp.setValue(manufacturer.getPhone());
            if (manufacturer.getFax() != null) {
                ContactPoint fax = org.addTelecom();
                fax.setSystem(ContactPoint.ContactPointSystem.FAX);
                fax.setUse(ContactPoint.ContactPointUse.WORK);
                fax.setValue(manufacturer.getFax());
            }
            createdResources.add(org);
        }
        return orgRef;
    }

    private void addSubsidy(ExtendedMedication medication, Subsidy subsidy) {
        SubsidyExtension subsidyExt = new SubsidyExtension();
        subsidyExt
            .setSubsidyCode(new Coding(FhirCodeSystemUri.PBS_SUBSIDY_URI.getUri(), subsidy.getPbsCode(), null));
        subsidyExt.setProgramCode(new Coding(FhirCodeSystemUri.PBS_PROGRAM_URI.getUri(), subsidy.getProgramCode(),
            PbsCodeSystemUtil.getProgramCodeDisplay(subsidy.getProgramCode())));
        subsidyExt.setCommonwealthExManufacturerPrice(new DecimalType(subsidy.getCommExManPrice()));
        subsidyExt.setManufacturerExManufacturerPrice(new DecimalType(subsidy.getManExManPrice()));
        subsidyExt
            .setRestriction(new Coding(FhirCodeSystemUri.PBS_RESTRICTION_URI.getUri(), subsidy.getRestriction(),
                PbsCodeSystemUtil.getRestrictionCodeDisplay(subsidy.getRestriction())));
        for (String note : subsidy.getNotes()) {
            subsidyExt.addNote(new Annotation(new StringType(note)));
        }
        for (String caution : subsidy.getCaution()) {
            subsidyExt.addCautionaryNote(new Annotation(new StringType(caution)));
        }

        for (Pair<String, String> atcCode : subsidy.getAtcCodes()) {
            CodeableConcept coding = subsidyExt.getAtcCode();
            if (coding == null) {
                coding = new CodeableConcept();
                subsidyExt.setAtcCode(coding);
            }
            Coding code = coding.addCoding();
            code.setCode(atcCode.getLeft());
            code.setDisplay(atcCode.getRight());
            code.setSystem(FhirCodeSystemUri.ATC_URI.getUri());
        }

        medication.getSubsidies().add(subsidyExt);
    }

    private Reference createProductResource(Concept concept, List<Resource> createdResources) {
        Reference reference = getExtendedMedicationReference(concept, new HashSet<>(), createdResources);
        if (!processedConcepts.contains(Long.toString(concept.getId()))) {
            processedConcepts.add(Long.toString(concept.getId()));
            ExtendedMedication medication = createBaseMedicationResource(concept, createdResources);
            concept.getRelationshipGroupsContaining(AttributeType.HAS_INTENDED_ACTIVE_INGREDIENT).forEach(
                r -> addIngredient(medication, r, createdResources));

            Concept form = concept.getSingleDestination(AttributeType.HAS_MANUFACTURED_DOSE_FORM);
            if (form != null) {
                medication.setForm(form.toCodeableConcept());
            }

            createdResources.add(medication);
        }
        return reference;
    }

    private void addProductReference(MedicationPackageComponent pkg, Relationship relationship,
            List<Resource> createdResources) {
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

            Set<Concept> destinationSet =
                    ctpp.stream()
                        .flatMap(c -> conceptCache.getConcept(c).getRelationships(relationship.getType()).stream())
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

        if (relationship.getType().equals(AttributeType.HAS_SUBPACK)
                || relationship.getType().equals(AttributeType.HAS_COMPONENT_PACK)) {
            content.setItem(createPackageResource(destination, createdResources));
        } else if (relationship.getType().equals(AttributeType.HAS_MPUU)
                || relationship.getType().equals(AttributeType.HAS_TPUU)) {
            content.setItem(createProductResource(destination, createdResources));
        }

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
            quantity.setSystem(FhirCodeSystemUri.SNOMED_CT_SYSTEM_URI.getUri());
        }

        content.setAmount(quantity);

    }

    private void addIngredient(Medication medication, Collection<Relationship> relationships,
            List<Resource> createdResources) {
        Relationship iai = relationships.stream()
            .filter(r -> r.getType().equals(AttributeType.HAS_INTENDED_ACTIVE_INGREDIENT))
            .findFirst()
            .get();

        MedicationIngredientComponentExtension ingredient = new MedicationIngredientComponentExtension();

        medication.addIngredient(ingredient);

        ingredient.setItem(createSubstanceResource(iai.getDestination(), createdResources));

        Relationship hasBoss = relationships.stream()
            .filter(r -> r.getType().equals(AttributeType.HAS_AUSTRALIAN_BOSS))
            .findFirst()
            .orElse(null);
        if (hasBoss != null) {
            if (hasBoss.getDestination().getId() != iai.getDestination().getId()) {
                ingredient.setBasisOfStrengthSubstance(hasBoss.getDestination().toCoding());
                createSubstanceResource(hasBoss.getDestination(), createdResources);
            }

            DataTypeProperty d = hasBoss.getDatatypeProperty();

            Concept denominatorUnit = d.getUnit().getSingleDestination(AttributeType.HAS_DENOMINATOR_UNITS);
            Concept numeratorUnit = d.getUnit().getSingleDestination(AttributeType.HAS_NUMERATOR_UNITS);

            Ratio value = new Ratio();

            Quantity denominator = new Quantity(1L);

            if (denominatorUnit != null) {
                denominator.setCode(Long.toString(denominatorUnit.getId()));
                denominator.setUnit(denominatorUnit.getPreferredTerm());
                denominator.setSystem(FhirCodeSystemUri.SNOMED_CT_SYSTEM_URI.getUri());
            }
            value.setDenominator(denominator);

            Quantity numerator = new Quantity(Double.parseDouble(d.getValue()));
            numerator.setCode(Long.toString(numeratorUnit.getId()));
            numerator.setUnit(numeratorUnit.getPreferredTerm());
            numerator.setSystem(FhirCodeSystemUri.SNOMED_CT_SYSTEM_URI.getUri());
            value.setNumerator(numerator);

            ingredient.setAmount(value);
        }
    }

    private Reference toReference(Concept concept, String resourceType) {
        Reference reference =
                new Reference(resourceType + "/" + concept.getId());
        reference.setDisplay(concept.getPreferredTerm());
        return reference;
    }

    private ExtendedMedicationReference getExtendedMedicationReference(Concept concept, Set<Long> addedConcepts,
            List<Resource> createdResources) {
        ExtendedMedicationReference reference;

        if (extendedMedicationReferenceCache.containsKey(concept.getId())) {
            reference = extendedMedicationReferenceCache.get(concept.getId());
        } else {
            reference = toExtendedMedicationReference(concept, addedConcepts, createdResources);
            extendedMedicationReferenceCache.put(concept.getId(), reference);
        }

        return reference;
    }

    private ExtendedMedicationReference toExtendedMedicationReference(Concept concept, Set<Long> addedConcepts,
            List<Resource> createdResources) {
        ExtendedMedicationReference reference =
                new ExtendedMedicationReference("Medication/" + concept.getId());
        reference.setDisplay(concept.getPreferredTerm());
        // addParentExtensions(concept, reference, addedConcepts, createdResources);
        reference.setType(concept.getMedicationType().getCode());
        reference.setStatus(
            new Enumeration<MedicationStatus>(new MedicationStatusEnumFactory(), concept.getMedicationStatus()));
        reference.setLastModified(new DateType(concept.getLastModified()));

        // addHistoicalAssociations(concept, reference, new HashSet<>(), createdResources);

        return reference;
    }

    private ExtendedSubstanceReference getExtendedSubstanceReference(Concept concept, Set<Long> addedConcepts,
            List<Resource> createdResources) {
        ExtendedSubstanceReference reference;

        if (extendedSubstanceReferenceCache.containsKey(concept.getId())) {
            reference = extendedSubstanceReferenceCache.get(concept.getId());
        } else {
            reference = toExtendedSubstanceReference(concept, addedConcepts, createdResources);
            extendedSubstanceReferenceCache.put(concept.getId(), reference);
        }
        return reference;
    }

    private ExtendedSubstanceReference toExtendedSubstanceReference(Concept concept, Set<Long> addedConcepts,
            List<Resource> createdResources) {
        ExtendedSubstanceReference reference =
                new ExtendedSubstanceReference("Substance/" + concept.getId());
        reference.setDisplay(concept.getPreferredTerm());
        reference.setStatus(
            new Enumeration<SubstanceStatus>(new SubstanceStatusEnumFactory(), concept.getSubstanceStatus()));
        reference.setLastModified(new DateType(concept.getLastModified()));

        // addHistoicalAssociations(concept, reference, new HashSet<>(), createdResources);

        return reference;
    }
}
