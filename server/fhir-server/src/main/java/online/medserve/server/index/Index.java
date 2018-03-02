package online.medserve.server.index;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.MMapDirectory;
import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.NumberAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import online.medserve.extension.ExtendedMedication;
import online.medserve.server.indexbuilder.constants.FieldNames;
import online.medserve.server.indexbuilder.constants.ResourceTypes;

public class Index {

    public static final String INDEX_LOCATION = System.getProperty("index.path", "/index");
    private IndexReader reader;
    private IndexSearcher searcher;

    public Index() throws IOException {
        this.reader = DirectoryReader.open(new MMapDirectory(Paths.get(INDEX_LOCATION)));
        this.searcher = new IndexSearcher(reader);
    }

    public <T extends BaseResource> T getResourceById(Class<T> clazz, String id) throws IOException {
        String resourceType = clazz.getSimpleName().replace("Extended", "").toLowerCase();
        Builder builder = new BooleanQuery.Builder()
            .add(new TermQuery(new Term(FieldNames.RESOURCE_TYPE, resourceType)), Occur.FILTER)
            .add(new TermQuery(new Term(FieldNames.ID, id)), Occur.FILTER);

        return getSingleResource(clazz, builder.build(), id);
    }

    public int getResourcesByCodeSize(Class<? extends BaseResource> clazz, TokenParam code) throws IOException {
        Query query = getResourcesByCodeQuery(clazz, code);
        return searcher.count(query);
    }

    public List<IBaseResource> getResourcesByCode(Class<? extends BaseResource> clazz, TokenParam code,
            int theFromIndex,
            int theToIndex) {

        Query query = getResourcesByCodeQuery(clazz, code);

        return getResources(clazz, theFromIndex, theToIndex, query);
    }

    private BooleanQuery getResourcesByCodeQuery(Class<? extends BaseResource> clazz, TokenParam code) {
        String resourceType = clazz.getSimpleName().replace("Extended", "").toLowerCase();
        Builder builder = new BooleanQuery.Builder()
            .add(new TermQuery(new Term(FieldNames.RESOURCE_TYPE, resourceType)), Occur.FILTER);

        QueryBuilder.searchCodableConcept(code, builder, FieldNames.CODE, Occur.MUST);

        return builder.build();
    }

    public int getResourcesByTextSize(Class<? extends BaseResource> clazz, TokenAndListParam code,
            StringAndListParam text, StringOrListParam status, DateAndListParam lastModified) throws IOException {
        return searcher.count(QueryBuilder.createTextSearchBuilder(clazz, code, text, status, lastModified).build());
    }

    public List<IBaseResource> getResourcesByText(Class<? extends BaseResource> clazz, TokenAndListParam code,
            StringAndListParam text,
            StringOrListParam status, DateAndListParam lastModified, int theFromIndex, int theToIndex) {
        return getResources(clazz, theFromIndex, theToIndex,
            QueryBuilder.createTextSearchBuilder(clazz, code, text, status, lastModified).build());
    }

    public int getMedicationsByParametersSize(Class<ExtendedMedication> clazz, TokenAndListParam code,
            StringAndListParam text,
            TokenAndListParam parent, TokenAndListParam ancestor, StringOrListParam medicationResourceType,
            TokenAndListParam form, TokenAndListParam container, TokenAndListParam ingredient,
            TokenAndListParam packageItem, TokenAndListParam brand, String isBrand, TokenAndListParam manufacturer,
            TokenAndListParam subsidyCode, StringOrListParam status, DateAndListParam lastModified,
            NumberAndListParam ingredientCount)
            throws IOException {
        Query query = getMedicationsByParametersQuery(clazz, code, text, parent, ancestor, medicationResourceType, form,
            container, ingredient, packageItem, brand, isBrand, manufacturer, subsidyCode, status, lastModified,
            ingredientCount);
        return searcher.count(query);
    }

    public List<IBaseResource> getMedicationsByParameters(Class<ExtendedMedication> clazz, TokenAndListParam code,
            StringAndListParam text,
            TokenAndListParam parent, TokenAndListParam ancestor, StringOrListParam medicationResourceType,
            TokenAndListParam form, TokenAndListParam container, TokenAndListParam ingredient,
            TokenAndListParam packageItem, TokenAndListParam brand, String isBrand, TokenAndListParam manufacturer,
            TokenAndListParam subsidyCode, StringOrListParam status, DateAndListParam lastModified,
            NumberAndListParam ingredientCount, int theFromIndex,
            int theToIndex) {

        Query query = getMedicationsByParametersQuery(clazz, code, text, parent, ancestor, medicationResourceType, form,
            container, ingredient, packageItem, brand, isBrand, manufacturer, subsidyCode, status, lastModified,
            ingredientCount);

        return getResources(clazz, theFromIndex, theToIndex, query);
    }

    private BooleanQuery getMedicationsByParametersQuery(Class<ExtendedMedication> clazz, TokenAndListParam code,
            StringAndListParam text, TokenAndListParam parent, TokenAndListParam ancestor,
            StringOrListParam medicationResourceType, TokenAndListParam form, TokenAndListParam container,
            TokenAndListParam ingredient, TokenAndListParam packageItem, TokenAndListParam brand, String isBrand,
            TokenAndListParam manufacturer, TokenAndListParam subsidyCode, StringOrListParam status,
            DateAndListParam lastModified, NumberAndListParam ingredientCount) {

        Builder builder = QueryBuilder.createTextSearchBuilder(clazz, code, text, status, lastModified);

        QueryBuilder.addOptionalNumberAndList(ingredientCount, builder, FieldNames.INGREDIENT_COUNT);

        QueryBuilder.addOptionalReferenceAndList(parent, builder, FieldNames.PARENT,
            ResourceTypes.MEDICATION_RESOURCE_TYPE_VALUE);

        QueryBuilder.addOptionalReferenceAndList(ancestor, builder, FieldNames.ANCESTOR,
            ResourceTypes.MEDICATION_RESOURCE_TYPE_VALUE);

        QueryBuilder.addOptionalStringOrList(medicationResourceType, builder, FieldNames.MEDICATION_RESOURCE_TYPE);

        QueryBuilder.addOptionalCodableConceptAndList(form, builder, FieldNames.FORM);

        QueryBuilder.addOptionalReferenceAndList(ingredient, builder, FieldNames.INGREDIENT,
            ResourceTypes.SUBSTANCE_RESOURCE_TYPE_VALUE);

        QueryBuilder.addOptionalCodableConceptAndList(container, builder, FieldNames.CONTAINER);

        QueryBuilder.addOptionalReferenceAndList(packageItem, builder, FieldNames.PACKAGE_ITEM,
            ResourceTypes.MEDICATION_RESOURCE_TYPE_VALUE);

        QueryBuilder.addOptionalCodableConceptAndList(brand, builder, FieldNames.BRAND);

        QueryBuilder.addOptionalReferenceAndList(manufacturer, builder, FieldNames.MANUFACTURER,
            ResourceTypes.ORGANIZATION_RESOURCE_TYPE_VALUE);

        QueryBuilder.addOptionalCodableConceptAndList(subsidyCode, builder, FieldNames.SUBSIDY_CODE);

        if (isBrand != null) {
            builder.add(new TermQuery(new Term(FieldNames.IS_BRAND, "" + Boolean.parseBoolean(isBrand))),
                Occur.MUST);
        }

        return builder.build();
    }

    private List<IBaseResource> getResources(Class<? extends IBaseResource> clazz, int theFromIndex, int theToIndex,
            Query query) {
        List<IBaseResource> result = new ArrayList<>();
        TopDocs docs;
        try {
            docs = searcher.search(query, theToIndex);

            for (int i = theFromIndex; i < theToIndex; i++) {

                result.add(DocumentReader.getResourceFromDocument(reader.document(docs.scoreDocs[i].doc), clazz));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed searching index with query '" + query + "'", e);
        }

        return result;
    }

    private <T extends BaseResource> T getSingleResource(Class<T> clazz, Query query, String id) throws IOException {
        TopDocs docs;
        try {
            docs = searcher.search(query, 2);
        } catch (IOException e) {
            throw new RuntimeException("Failed searching index with query '" + query + "'", e);
        }

        if (docs.totalHits > 1) {
            throw new RuntimeException("More than one " + clazz.getSimpleName() + " resource found for id " + id);
        }

        if (docs.totalHits == 0) {
            return null;
        }
        return DocumentReader.getResourceFromDocument(reader.document(docs.scoreDocs[0].doc), clazz);
    }
}
