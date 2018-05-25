package au.gov.digitalhealth.medserve.transform.rxnorm;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.DecimalType;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.Medication.MedicationPackageComponent;
import org.hl7.fhir.dstu3.model.Medication.MedicationStatus;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Narrative.NarrativeStatus;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Substance;

import au.gov.digitalhealth.medserve.extension.ExtendedMedication;
import au.gov.digitalhealth.medserve.extension.SubsidyExtension;
import au.gov.digitalhealth.medserve.transform.processor.MedicationResourceProcessor;
import au.gov.digitalhealth.medserve.transform.rxnorm.cache.RxNormCache;
import au.gov.digitalhealth.medserve.transform.rxnorm.enumeration.RxNormReltaionshipType;
import au.gov.digitalhealth.medserve.transform.rxnorm.enumeration.RxNormType;
import au.gov.digitalhealth.medserve.transform.rxnorm.model.Concept;

public class RxNormMedicationResourceGenerator {
    private static final Logger logger = Logger.getLogger(RxNormMedicationResourceGenerator.class.getCanonicalName());
    private RxNormCache conceptCache;
    // private Map<String, Organization> organisations = new HashMap<>();

    public RxNormMedicationResourceGenerator(Path rxnormZipFilePath)
            throws IOException {
        this.conceptCache =
                new RxNormCache(getFileSystemForZipPath(rxnormZipFilePath));
    }

    private FileSystem getFileSystemForZipPath(Path path) throws IOException {
        return FileSystems.newFileSystem(
            URI.create("jar:file:" + path.toAbsolutePath().toString()),
            new HashMap<>());
    }

    public void process(MedicationResourceProcessor processor) throws IOException {
        processor.processResources(conceptCache.getConceptsOfType(RxNormType.Ingredient)
            .stream()
            .map(concept -> createSubstanceResource(concept))
            .collect(Collectors.toList()));
        logger.info("completed Substances");

        processor.processResources(conceptCache.getConceptsOfType(RxNormType.BrandedPack)
            .stream()
            .map(concept -> createPackageResource(concept))
            .collect(Collectors.toList()));
        logger.info("completed Substances");

        // processor.processResources(organisations.values(), "org");
        // logger.info("completed organisations");
    }

    private Substance createSubstanceResource(Concept concept) {
        Substance substance = new Substance();
        setStandardResourceElements(concept, substance);

        substance.setCode(concept.toCodeableConcept());
        // concept.getMultipleDestinations(AttributeType.IS_MODIFICATION_OF)
        // .forEach(m -> substance.addIngredient().setSubstance(toReference(m, "Substance")));

        return substance;
    }

    private ExtendedMedication createBaseMedicationResource(Concept concept) {
        ExtendedMedication medication = new ExtendedMedication();
        setStandardResourceElements(concept, medication);

        medication.setCode(concept.toCodeableConcept());

        medication.setMedicationResourceType(concept.getMedicationType().getCode());

        // TODO addParentExtensions(concept, medication, new HashSet<>());

        medication.setIsBrand(concept.getMedicationType().isBranded());

        if (concept.isActive()) {
            medication.setStatus(MedicationStatus.ACTIVE);
        } else {
            medication.setStatus(MedicationStatus.ENTEREDINERROR);
        }

        // make medication for GPCK
        getReferences(concept.getTargets(RxNormReltaionshipType.Contains, RxNormType.SemanticBrandedDrug));

        // attach SBD

        if (concept.hasAtLeastOneMatchingAncestor(AmtConcept.TP)) {
            Concept brand = concept.getAncestors(AmtConcept.TP).iterator().next();
            medication.setBrand(brand.toCodeableConcept());
        } else if (concept.hasAtLeastOneMatchingAncestor(AmtConcept.TPP)) {
            Concept brand = concept.getSingleDestination(AttributeType.HAS_TP);
            medication.setBrand(brand.toCodeableConcept());
        }

        return medication;
    }

    private void getReferences(Set<Concept> targets) {

    }

    //
    private void setStandardResourceElements(Concept concept, DomainResource resource) {
        resource.setId(Long.toString(concept.getRxcui()));
        Narrative narrative = new Narrative();
        narrative.setStatus(NarrativeStatus.GENERATED);
        narrative.setDivAsString("<div><p>" + StringEscapeUtils.escapeHtml3(concept.getName()) + "</p></div>");
        resource.setText(narrative);
    }
    //
    // private void addParentExtensions(Concept concept, ExtendedMedication resource, Set<Long> addedConcepts) {
    // concept.getParents()
    // .values()
    // .stream()
    // .filter(parent -> !AmtConcept.isEnumValue(Long.toString(parent.getId())))
    // .filter(parent -> !addedConcepts.contains(parent.getId()))
    // .forEach(parent -> {
    // if (!parent.hasParent(AmtConcept.TP)) {
    // MedicationParentExtension extension = new MedicationParentExtension();
    // extension.setParentMedication(toReference(parent, "Medication"));
    // extension.setMedicationResourceType(MedicationType.getMedicationType(parent).getCode());
    //
    // resource.addParentMedicationResources(extension);
    //
    // addedConcepts.add(parent.getId());
    // addParentExtensions(parent, resource, addedConcepts);
    // }
    // });
    // }
    //
    private Medication createPackageResource(Concept concept) {
        ExtendedMedication medication = createBaseMedicationResource(concept);
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

        for (Subsidy subsidy : concept.getSubsidies()) {
            SubsidyExtension subsidyExt = new SubsidyExtension();
            subsidyExt.setSubsidyCode(new Coding("http://pbs.gov.au/schedule", subsidy.getPbsCode(), null));
            subsidyExt.setProgramCode(new Coding("http://pbs.gov.au/programCode", subsidy.getProgramCode(),
                getProgramCodeDisplay(subsidy.getProgramCode())));
            subsidyExt.setCommonwealthExManufacturerPrice(new DecimalType(subsidy.getCommExManPrice()));
            subsidyExt.setManufacturerExManufacturerPrice(new DecimalType(subsidy.getManExManPrice()));
            subsidyExt.setRestriction(new Coding("http://pbs.gov.au/restriction", subsidy.getRestriction(),
                getRestrictionCodeDisplay(subsidy.getRestriction())));
            for (String note : subsidy.getNotes()) {
                subsidyExt.addNote(new Annotation(new StringType(note)));
            }
            for (String caution : subsidy.getCaution()) {
                subsidyExt.addCautionaryNote(new Annotation(new StringType(caution)));
            }
            medication.getSubsidies().add(subsidyExt);
        }

        Manufacturer manufacturer = concept.getManufacturer();
        if (manufacturer != null) {
            if (!organisations.containsKey(manufacturer.getCode())) {
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
                organisations.put(manufacturer.getCode(), org);
            }
            Reference orgRef = new Reference("Organization/" + manufacturer.getCode());
            orgRef.setDisplay(manufacturer.getName());
            medication.setManufacturer(orgRef);
        }

        for (Pair<String, String> atcCode : concept.getAtcCodes()) {
            Coding code = medication.getCode().addCoding();
            code.setCode(atcCode.getLeft());
            code.setDisplay(atcCode.getRight());
            code.setSystem("http://www.whocc.no/atc");
        }

        return medication;
    }
    //
    // private String getRestrictionCodeDisplay(String restriction) {
    // switch (restriction) {
    // case "U":
    // return "Unrestricted";
    // case "R":
    // return "Restricted benefit";
    // case "A":
    // return "Authority required";
    //
    // default:
    // throw new RuntimeException("No display for code " + restriction);
    // }
    // }
    //
    // private String getProgramCodeDisplay(String programCode) {
    // switch (programCode) {
    // case "EP":
    // return "Extemporaneous Preparations";
    // case "GE":
    // return "Generally Available Pharmaceutical Benefits";
    // case "PL":
    // return "Palliative Care";
    // case "DB":
    // return "Prescriber Bag";
    // case "R1":
    // return "Repatriation Pharmaceutical Benefits Scheme only";
    // case "MF":
    // return "Botulinum Toxin Program";
    // case "IN":
    // return "Efficient Funding of Chemotherapy - Private Hospital – infusibles";
    // case "IP":
    // return "Efficient Funding of Chemotherapy - Public Hospital - infusibles";
    // case "CT":
    // return "Efficient Funding of Chemotherapy - Related Benefits";
    // case "TY":
    // return "Efficient Funding of Chemotherapy - Private Hospital - Trastuzumab";
    // case "TZ":
    // return "Efficient Funding of Chemotherapy - Public Hospital - Trastuzumab";
    // case "GH":
    // return "Growth Hormone Program";
    // case "HS":
    // return "Highly Specialised Drugs Program - Private Hospital";
    // case "HB":
    // return "Highly Specialised Drugs Program - Public Hospital";
    // case "CA":
    // return "Highly Specialised Drugs Program - Community Access";
    // case "IF":
    // return "IVF Program";
    // case "MD":
    // return "Opiate Dependence Treatment Program";
    // case "PQ":
    // return "Paraplegic and Quadriplegic Program";
    //
    // default:
    // throw new RuntimeException("No display for code " + programCode);
    // }
    // }
    //
    // private Medication createProductResource(Concept concept) {
    // ExtendedMedication medication = createBaseMedicationResource(concept);
    // concept.getRelationshipGroupsContaining(AttributeType.HAS_INTENDED_ACTIVE_INGREDIENT).forEach(
    // r -> addIngredient(medication, r));
    //
    // Concept form = concept.getSingleDestination(AttributeType.HAS_MANUFACTURED_DOSE_FORM);
    // if (form != null) {
    // medication.setForm(form.toCodeableConcept());
    // }
    // return medication;
    // }
    //
    // private void addProductReference(MedicationPackageComponent pkg, Relationship relationship) {
    // MedicationPackageContentComponent content = pkg.addContent();
    //
    // Concept destination = null;
    // // This is a dirty hack because AMT doesn't restate the
    // // HAS_COMPONENT_PACK and HAS_SUBPACK relationships from MPP on TPP. As
    // // a result these relationships on TPPs are the inferred relationships
    // // from MPP which target MPPs not TPPs. This code tries to back stitch
    // // to the TPP which should be the target of a stated HAS_SUBPACK or
    // // HAS_COMPONENT_PACK relationship, but the real solution is to state
    // // these in AMT
    // if ((relationship.getType().equals(AttributeType.HAS_SUBPACK)
    // || relationship.getType().equals(AttributeType.HAS_COMPONENT_PACK))
    // && relationship.getSource().hasParent(AmtConcept.TPP)) {
    // Collection<Long> ctpp = conceptCache.getDescendantOf(relationship.getSource().getId());
    // if (ctpp.size() != 1) {
    // throw new RuntimeException("More than one ctpp found for "
    // + relationship.getSource().toReference()
    // + " ctpps were " + StringUtils.join(ctpp, "'"));
    // }
    //
    // Set<Concept> destinationSet = conceptCache.getConcept(ctpp.iterator().next())
    // .getRelationships(relationship.getType())
    // .stream()
    // .flatMap(r -> r.getDestination().getParents().values().stream())
    // .filter(c -> c.hasParent(relationship.getDestination()))
    // .collect(Collectors.toSet());
    //
    // if (destinationSet.size() != 1) {
    // throw new RuntimeException("Destination set was expected to be 1 but was " + destinationSet);
    // }
    //
    // destination = destinationSet.iterator().next();
    //
    // } else {
    // destination = relationship.getDestination();
    // }
    //
    // content.setItem(toReference(destination, "Medication"));
    //
    // SimpleQuantity quantity;
    //
    // if (relationship.getType().equals(AttributeType.HAS_COMPONENT_PACK)) {
    // quantity = new SimpleQuantity();
    // quantity.setValue(1);
    // } else {
    // DataTypeProperty datatypeProperty = relationship.getDatatypeProperty();
    //
    // quantity = new SimpleQuantity();
    // quantity.setValue(Double.valueOf(datatypeProperty.getValue()));
    // quantity.setCode(Long.toString(datatypeProperty.getUnit().getId()));
    // quantity.setUnit(datatypeProperty.getUnit().getPreferredTerm());
    // quantity.setSystem(Concept.SNOMED_CT_SYSTEM_URI);
    // }
    //
    // content.setAmount(quantity);
    // }
    //
    // private void addIngredient(Medication medication, Collection<Relationship> relationships) {
    // Relationship iai = relationships.stream()
    // .filter(r -> r.getType().equals(AttributeType.HAS_INTENDED_ACTIVE_INGREDIENT))
    // .findFirst()
    // .get();
    //
    // MedicationIngredientComponentWithBossExtension ingredient =
    // new MedicationIngredientComponentWithBossExtension();
    //
    // medication.addIngredient(ingredient);
    //
    // ingredient.setItem(toReference(iai.getDestination(), "Substance"));
    //
    // Relationship hasBoss = relationships.stream()
    // .filter(r -> r.getType().equals(AttributeType.HAS_AUSTRALIAN_BOSS))
    // .findFirst()
    // .orElse(null);
    // if (hasBoss != null) {
    // if (hasBoss.getDestination().getId() != iai.getDestination().getId()) {
    // ingredient.setBasisOfStrengthSubstance(hasBoss.getDestination().toCoding());
    // }
    //
    // DataTypeProperty d = hasBoss.getDatatypeProperty();
    //
    // Concept denominatorUnit = d.getUnit().getSingleDestination(AttributeType.HAS_DENOMINATOR_UNITS);
    // Concept numeratorUnit = d.getUnit().getSingleDestination(AttributeType.HAS_NUMERATOR_UNITS);
    //
    // Ratio value = new Ratio();
    //
    // Quantity denominator = new Quantity(1L);
    //
    // if (denominatorUnit != null) {
    // denominator.setCode(Long.toString(denominatorUnit.getId()));
    // denominator.setUnit(denominatorUnit.getPreferredTerm());
    // denominator.setSystem(Concept.SNOMED_CT_SYSTEM_URI);
    // }
    // value.setDenominator(denominator);
    //
    // Quantity numerator = new Quantity(Double.parseDouble(d.getValue()));
    // numerator.setCode(Long.toString(numeratorUnit.getId()));
    // numerator.setUnit(numeratorUnit.getPreferredTerm());
    // numerator.setSystem(Concept.SNOMED_CT_SYSTEM_URI);
    // value.setNumerator(numerator);
    //
    // ingredient.setAmount(value);
    // }
    // }
    //
    // private Reference toReference(Concept concept, String resourceType) {
    // Reference substanceReference =
    // new Reference(resourceType + "/" + concept.getId());
    // substanceReference.setDisplay(concept.getPreferredTerm());
    // return substanceReference;
    // }

}
