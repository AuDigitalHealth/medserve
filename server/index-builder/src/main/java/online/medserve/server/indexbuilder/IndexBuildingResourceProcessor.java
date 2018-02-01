package online.medserve.server.indexbuilder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Medication.MedicationIngredientComponent;
import org.hl7.fhir.dstu3.model.Medication.MedicationPackageContentComponent;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.Substance;
import org.hl7.fhir.exceptions.FHIRException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import online.medserve.extension.ExtendedMedication;
import online.medserve.extension.ExtendedSubstance;
import online.medserve.extension.MedicationParentExtension;
import online.medserve.extension.ParentExtendedElement;
import online.medserve.extension.SubsidyExtension;
import online.medserve.server.indexbuilder.constants.FieldNames;
import online.medserve.server.indexbuilder.constants.ResourceTypes;
import online.medserve.transform.processor.MedicationResourceProcessor;

public class IndexBuildingResourceProcessor implements MedicationResourceProcessor {

    private IndexWriter writer;
    private IParser parser;
    private Map<String, CodeableConcept> formCache = new HashMap<>();
    private Map<String, Set<Reference>> ingredientCache = new HashMap<>();

    public IndexBuildingResourceProcessor(File outputDirectory) throws IOException {

        parser = FhirContext.forDstu3().newJsonParser();
        parser.setPrettyPrint(false);

        Directory dir = FSDirectory.open(outputDirectory.toPath());
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        this.writer = new IndexWriter(dir, iwc);
    }

    @Override
    public void processResources(List<? extends Resource> resources) throws IOException {
        for (Resource resource : resources) {
            Document document = new Document();

            document.add(new StringField(FieldNames.ID, resource.getId(), Store.YES));
            document.add(
                new StringField(FieldNames.RESOURCE_TYPE, resource.getResourceType().name().toLowerCase(), Store.NO));
            String text = ((DomainResource) resource).getText().getDiv().allText();

            document.add(new TextField(FieldNames.DISPLAY, text, Store.NO));

            if (resource instanceof ExtendedMedication) {
                indextMedicationResource(resource, document);
            } else if (resource instanceof Substance) {
                ExtendedSubstance substance = ExtendedSubstance.class.cast(resource);
                document.add(
                    new StringField(FieldNames.STATUS, substance.getStatus().toCode(), Store.NO));
                document.add(
                    new StringField(FieldNames.LAST_MODIFIED, substance.getLastModified().asStringValue(), Store.NO));
                indexCodeableConcept(document, substance.getCode(), FieldNames.CODE);
            } else if (resource instanceof Organization) {
                // nothing special here
            } else {
                throw new RuntimeException("Unknown resource type " + resource.getClass().getCanonicalName());
            }
            document.add(new StoredField(FieldNames.JSON, parser.encodeResourceToString(resource)));

            writer.addDocument(document);
        }
        writer.commit();
    }

    private void indextMedicationResource(Resource resource, Document document) {
        ExtendedMedication medication = ExtendedMedication.class.cast(resource);

        document.add(
            new StringField(FieldNames.STATUS, medication.getStatus().toCode(), Store.NO));

        document.add(
            new StringField(FieldNames.LAST_MODIFIED, medication.getLastModified().asStringValue(), Store.NO));

        indexCodeableConcept(document, medication.getCode(), FieldNames.CODE);

        document.add(new StringField(FieldNames.MEDICATION_RESOURCE_TYPE,
            medication.getMedicationResourceType().getCode(), Store.NO));

        indexParents(document, medication, FieldNames.ANCESTOR);
        indexParents(document, medication, FieldNames.PARENT);

        if (medication.hasIsBrand()) {
            document.add(new StringField(FieldNames.IS_BRAND, Boolean.toString(medication.getIsBrand()), Store.NO));
            if (medication.getBrand() != null) {
                indexCodeableConcept(document, medication.getBrand(), FieldNames.BRAND);
            }
        }

        if (medication.getForm() != null && !medication.getForm().isEmpty()) {
            indexCodeableConcept(document, medication.getForm(), FieldNames.FORM);
            formCache.put(resource.getId(), medication.getForm());
        }

        if (medication.getIngredient() != null && !medication.getIngredient().isEmpty()) {
            for (MedicationIngredientComponent ingredient : medication.getIngredient()) {
                try {
                    indexReference(document, ingredient.getItemReference(), FieldNames.INGREDIENT,
                        ResourceTypes.SUBSTANCE_RESOURCE_TYPE_VALUE);
                    cacheIngredient(resource.getId(), ingredient.getItemReference());
                } catch (FHIRException e) {
                    throw new RuntimeException("Cannot get reference for ingredient " + ingredient);
                }
            }
            document.add(new IntPoint(FieldNames.INGREDIENT_COUNT, medication.getIngredient().size()));
        }

        if (medication.getPackage() != null) {
            if (medication.getPackage().getContainer() != null && !medication.getPackage().getContainer().isEmpty()) {
                indexCodeableConcept(document, medication.getPackage().getContainer(), FieldNames.CONTAINER);
            }
            if (medication.getPackage().getContent() != null && !medication.getPackage().getContent().isEmpty()) {
                Set<String> distinctIngredientSet = new HashSet<>();
                for (MedicationPackageContentComponent content : medication.getPackage().getContent()) {
                    try {
                        indexReference(document, content.getItemReference(), FieldNames.PACKAGE_ITEM,
                            ResourceTypes.MEDICATION_RESOURCE_TYPE_VALUE);
                        indexCodeableConcept(document, formCache.get(getIdFromReference(content.getItemReference(),
                            ResourceTypes.MEDICATION_RESOURCE_TYPE_VALUE)), FieldNames.FORM);
                        formCache.put(resource.getId(), formCache.get(getIdFromReference(content.getItemReference(),
                            ResourceTypes.MEDICATION_RESOURCE_TYPE_VALUE)));

                        Set<Reference> ingredientSet =
                                ingredientCache.get(getIdFromReference(content.getItemReference(),
                                    ResourceTypes.MEDICATION_RESOURCE_TYPE_VALUE));
                        distinctIngredientSet
                            .addAll(ingredientSet.stream().map(r -> r.getId()).collect(Collectors.toSet()));

                        for (Reference ingredientReference : ingredientSet) {
                            indexReference(document, ingredientReference, FieldNames.INGREDIENT,
                                ResourceTypes.SUBSTANCE_RESOURCE_TYPE_VALUE);

                            cacheIngredient(resource.getId(), ingredientReference);
                        }
                        
                    } catch (FHIRException e) {
                        throw new RuntimeException("Cannot get reference for package-item " + content);
                    }
                }

                document.add(new IntPoint(FieldNames.INGREDIENT_COUNT, distinctIngredientSet.size()));
            }
        }

        if (medication.hasManufacturer()) {
            try {
                indexReference(document, medication.getManufacturer(), FieldNames.MANUFACTURER,
                    ResourceTypes.ORGANIZATION_RESOURCE_TYPE_VALUE);
            } catch (FHIRException e) {
                throw new RuntimeException("Can't get manufacturer for meducation " + medication.getId());
            }
        }

        if (medication.getSubsidies() != null && !medication.getSubsidies().isEmpty()) {
            for (SubsidyExtension subsidy : medication.getSubsidies()) {
                indexCoding(document, FieldNames.SUBSIDY_CODE, subsidy.getSubsidyCode());
            }
        }
    }

    private void cacheIngredient(String id, Reference itemReference) {
        Set<Reference> ingredients = ingredientCache.get(id);
        if (ingredients == null) {
            ingredients = new HashSet<>();
            ingredientCache.put(id, ingredients);
        }
        if (!ingredients.stream().anyMatch(r -> r.getReference().equals(itemReference.getReference()))) {
            ingredients.add(itemReference);
        }
    }

    private void indexParents(Document document, ParentExtendedElement medication, String fieldName) {
        for (MedicationParentExtension parent : medication.getParentMedicationResources()) {
            try {
                indexReference(document, parent.getParentMedication(), fieldName,
                    ResourceTypes.MEDICATION_RESOURCE_TYPE_VALUE);
                if (fieldName.equals(FieldNames.ANCESTOR)) {
                    indexParents(document, parent, fieldName);
                }
            } catch (FHIRException e) {
                throw new RuntimeException("Cannot get reference for medication abstraction " + parent, e);
            }
        }
    }

    private void indexReference(Document document, Reference reference, String fieldName, String referenceType)
            throws FHIRException {
        document.add(new StringField(fieldName, getIdFromReference(reference, referenceType), Store.NO));
        document.add(new TextField(fieldName + FieldNames.TEXT_FIELD_SUFFIX, reference.getDisplay(), Store.NO));
    }

    private String getIdFromReference(Reference reference, String referenceType) {
        return reference.getReference().replace(referenceType + "/", "");
    }

    private void indexCodeableConcept(Document document, CodeableConcept codableConcept, String fieldName) {
        for (Coding code : codableConcept.getCoding()) {
            indexCoding(document, fieldName, code);
        }
        if (codableConcept.getText() != null && !codableConcept.getText().isEmpty()) {
            document.add(new TextField(fieldName + FieldNames.TEXT_FIELD_SUFFIX, codableConcept.getText(), Store.NO));
        }
    }

    private void indexCoding(Document document, String fieldName, Coding code) {
        document.add(new StringField(fieldName, code.getCode() + "|" + code.getSystem(), Store.NO));
        if (code.getDisplay() != null && !code.getDisplay().isEmpty()) {
            document.add(new TextField(fieldName + FieldNames.TEXT_FIELD_SUFFIX, code.getDisplay(), Store.NO));
        }
    }

}
