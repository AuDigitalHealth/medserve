package online.medserve.server.index;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.hl7.fhir.dstu3.model.BaseResource;

import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import online.medserve.server.indexbuilder.constants.FieldNames;

public final class QueryBuilder {

    private QueryBuilder() {
        throw new AssertionError("Static method helper class not to be constructed!");
    }

    public static void addCodesearch(TokenParam code, Builder builder, String fieldName, Occur occur) {
        String system = code.getSystem();
        String codeValue = code.getValue();
    
        if (system != null && !system.isEmpty()) {
            builder.add(new TermQuery(new Term(fieldName, codeValue + "|" + system)), occur);
        } else {
            Builder subquery = new BooleanQuery.Builder();
            subquery.add(new PrefixQuery(new Term(fieldName, codeValue + "|")), Occur.SHOULD);
            if (fieldName.equals(FieldNames.CODE)) {
                subquery.add(new PrefixQuery(new Term(FieldNames.ID, codeValue)), Occur.SHOULD);
            }
            builder.add(subquery.build(), occur);
        }
    }

    public static void addOptionalCodableConceptAndList(TokenAndListParam codeableConcept, Builder builder,
            String string) {
        if (codeableConcept != null) {
            List<TokenOrListParam> codeQueries = codeableConcept.getValuesAsQueryTokens();
            for (TokenOrListParam codeQuery : codeQueries) {
                Builder subAndQuery = new BooleanQuery.Builder();
                List<TokenParam> queryTokens = codeQuery.getValuesAsQueryTokens();
                // Only return results that match at least one of the tokens in the list below
                boolean mustnot = false;
                for (TokenParam nextString : queryTokens) {
                    mustnot = mustnot
                            || (nextString.getModifier() != null
                                    && nextString.getModifier().equals(TokenParamModifier.NOT));
                    searchCodableConcept(nextString, subAndQuery, string, Occur.SHOULD);
                }
                builder.add(subAndQuery.build(), mustnot ? Occur.MUST_NOT : Occur.MUST);
            }
        }
    }

    public static void addOptionalReferenceAndList(TokenAndListParam reference, Builder builder, String fieldName,
            String type) {
        if (reference != null) {
            List<TokenOrListParam> refQueries = reference.getValuesAsQueryTokens();
            for (TokenOrListParam refQuery : refQueries) {
                Builder subAndQuery = new BooleanQuery.Builder();
                List<TokenParam> queryTokens = refQuery.getValuesAsQueryTokens();
                // Only return results that match at least one of the tokens in the list below
                boolean mustnot = false;
                for (TokenParam nextString : queryTokens) {
                    mustnot = mustnot
                            || (nextString.getModifier() != null
                                    && nextString.getModifier().equals(TokenParamModifier.NOT));
                    searchReference(nextString, subAndQuery, fieldName, type, Occur.SHOULD);
                }
                builder.add(subAndQuery.build(), mustnot ? Occur.MUST_NOT : Occur.MUST);
            }
        }
    }

    public static void addOptionalStringOrList(StringOrListParam param, Builder builder, String fieldName) {
        if (param != null) {
            List<StringParam> queryTokens = param.getValuesAsQueryTokens();
            // Only return results that match at least one of the tokens in the list below
    
            Builder subAndQuery = new BooleanQuery.Builder();
            for (StringParam nextString : queryTokens) {
                subAndQuery.add(new TermQuery(new Term(fieldName, nextString.getValue())),
                    Occur.SHOULD);
            }
            builder.add(subAndQuery.build(), Occur.MUST);
        }
    }

    // public <T extends BaseResource> List<T> getResources(Class<T> clazz, StringAndListParam text, int count) {
    // Builder builder = createTextSearchBuilder(clazz, text);
    // return getResources(clazz, count, builder.build());
    // }
    
    // public <T extends BaseResource> List<T> getResources(Class<T> clazz, StringAndListParam text,
    // TokenAndListParam parent, StringOrListParam medicationResourceType, TokenAndListParam form,
    // TokenAndListParam container, TokenAndListParam ingredient, TokenAndListParam packageItem,
    // TokenAndListParam brand, String isBrand, TokenAndListParam manufacturer, TokenAndListParam subsidyCode,
    // int count) {
    //
    // Builder builder = createTextSearchBuilder(clazz, text);
    //
    // addOptionalReferenceAndList(parent, builder, FieldNames.PARENT, ResourceTypes.MEDICATION_RESOURCE_TYPE_VALUE);
    //
    // addOptionalStringOrList(medicationResourceType, builder, FieldNames.MEDICATION_RESOURCE_TYPE);
    //
    // addOptionalCodableConceptAndList(form, builder, FieldNames.FORM);
    //
    // addOptionalReferenceAndList(ingredient, builder, FieldNames.INGREDIENT,
    // ResourceTypes.SUBSTANCE_RESOURCE_TYPE_VALUE);
    //
    // addOptionalCodableConceptAndList(container, builder, FieldNames.CONTAINER);
    //
    // addOptionalReferenceAndList(packageItem, builder, FieldNames.PACKAGE_ITEM,
    // ResourceTypes.MEDICATION_RESOURCE_TYPE_VALUE);
    //
    // addOptionalCodableConceptAndList(brand, builder, FieldNames.BRAND);
    //
    // addOptionalReferenceAndList(manufacturer, builder, FieldNames.MANUFACTURER,
    // ResourceTypes.ORGANIZATION_RESOURCE_TYPE_VALUE);
    //
    // addOptionalCodableConceptAndList(subsidyCode, builder, FieldNames.SUBSIDY_CODE);
    //
    // if (isBrand != null) {
    // builder.add(new TermQuery(new Term(FieldNames.IS_BRAND, "" + Boolean.parseBoolean(isBrand))),
    // Occur.MUST);
    // }
    //
    // return getResources(clazz, count, builder.build());
    //
    // }
    
    public static <T extends BaseResource> Builder createTextSearchBuilder(Class<T> clazz, StringAndListParam text) {
        String resourceType = clazz.getSimpleName().replace("Extended", "").toLowerCase();
        Builder builder = new BooleanQuery.Builder()
            .add(new TermQuery(new Term(FieldNames.RESOURCE_TYPE, resourceType)), Occur.FILTER);
    
        if (text != null) {
            List<StringOrListParam> queries = text.getValuesAsQueryTokens();
    
            for (StringOrListParam query : queries) {
                Builder subAndQuery = new BooleanQuery.Builder();
                List<StringParam> queryTokens = query.getValuesAsQueryTokens();
                // Only return results that match at least one of the tokens in the list below
                for (StringParam nextString : queryTokens) {
                    Builder subAndQuery2 = new BooleanQuery.Builder();
                    Arrays.stream(nextString.getValue().split(" "))
                        .forEach(
                            s -> subAndQuery2.add(new PrefixQuery(new Term(FieldNames.DISPLAY, s.toLowerCase())),
                                Occur.MUST));
    
                    subAndQuery.add(subAndQuery2.build(), Occur.SHOULD);
                }
                builder.add(subAndQuery.build(), Occur.MUST);
            }
        }
        return builder;
    }

    public static void searchCodableConcept(TokenParam code, Builder builder, String fieldName, Occur occur) {
        if (code.getModifier() == null || code.getModifier().equals(TokenParamModifier.NOT)) {
            addCodesearch(code, builder, fieldName, occur);
        } else if (code.getModifier().equals(TokenParamModifier.TEXT)) {
            Builder subquery = new BooleanQuery.Builder();
            Arrays.stream(code.getValue().split(" "))
                .forEach(
                    s -> subquery.add(
                        new PrefixQuery(new Term(fieldName + FieldNames.TEXT_FIELD_SUFFIX, s.toLowerCase())),
                        Occur.MUST));
            builder.add(subquery.build(), occur);
        } else {
            throw new NotImplementedOperationException(
                "Modifier " + code.getModifier().getValue() + " is not yet supported ...sorry");
        }
    }

    // public <T extends BaseResource> List<T> getResources(Class<T> clazz, StringAndListParam text, int count) {
    // Builder builder = createTextSearchBuilder(clazz, text);
    // return getResources(clazz, count, builder.build());
    // }
    
    // public <T extends BaseResource> List<T> getResources(Class<T> clazz, StringAndListParam text,
    // TokenAndListParam parent, StringOrListParam medicationResourceType, TokenAndListParam form,
    // TokenAndListParam container, TokenAndListParam ingredient, TokenAndListParam packageItem,
    // TokenAndListParam brand, String isBrand, TokenAndListParam manufacturer, TokenAndListParam subsidyCode,
    // int count) {
    //
    // Builder builder = createTextSearchBuilder(clazz, text);
    //
    // addOptionalReferenceAndList(parent, builder, FieldNames.PARENT, ResourceTypes.MEDICATION_RESOURCE_TYPE_VALUE);
    //
    // addOptionalStringOrList(medicationResourceType, builder, FieldNames.MEDICATION_RESOURCE_TYPE);
    //
    // addOptionalCodableConceptAndList(form, builder, FieldNames.FORM);
    //
    // addOptionalReferenceAndList(ingredient, builder, FieldNames.INGREDIENT,
    // ResourceTypes.SUBSTANCE_RESOURCE_TYPE_VALUE);
    //
    // addOptionalCodableConceptAndList(container, builder, FieldNames.CONTAINER);
    //
    // addOptionalReferenceAndList(packageItem, builder, FieldNames.PACKAGE_ITEM,
    // ResourceTypes.MEDICATION_RESOURCE_TYPE_VALUE);
    //
    // addOptionalCodableConceptAndList(brand, builder, FieldNames.BRAND);
    //
    // addOptionalReferenceAndList(manufacturer, builder, FieldNames.MANUFACTURER,
    // ResourceTypes.ORGANIZATION_RESOURCE_TYPE_VALUE);
    //
    // addOptionalCodableConceptAndList(subsidyCode, builder, FieldNames.SUBSIDY_CODE);
    //
    // if (isBrand != null) {
    // builder.add(new TermQuery(new Term(FieldNames.IS_BRAND, "" + Boolean.parseBoolean(isBrand))),
    // Occur.MUST);
    // }
    //
    // return getResources(clazz, count, builder.build());
    //
    // }
    
    public static void searchReference(TokenParam reference, Builder builder, String fieldName, String type,
            Occur occur) {
        if (reference.getModifier() == null || reference.getModifier().equals(TokenParamModifier.NOT)) {
            builder.add(new TermQuery(new Term(fieldName, reference.getValue().replace(type + "/", ""))), occur);
        } else if (reference.getModifier().equals(TokenParamModifier.TEXT)) {
            builder.add(
                new PrefixQuery(new Term(fieldName + FieldNames.TEXT_FIELD_SUFFIX, reference.getValue().toLowerCase())),
                occur);
        } else {
            throw new NotImplementedOperationException(
                "Modifier " + reference.getModifier().getValue() + " is not yet supported ...sorry");
        }
    }
}
