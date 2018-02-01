package online.medserve.server.index;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.hl7.fhir.dstu3.model.BaseResource;

import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateOrListParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
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

    public static void addOptionalDateAndList(DateAndListParam lastModified, Builder builder, String fieldName) {
        if (lastModified != null) {
            List<DateOrListParam> dateQueries = lastModified.getValuesAsQueryTokens();
            for (DateOrListParam dateQuery : dateQueries) {
                Builder subAndQuery = new BooleanQuery.Builder();
                List<DateParam> queryTokens = dateQuery.getValuesAsQueryTokens();
                // Only return results that match at least one of the tokens in the list below
                for (DateParam date : queryTokens) {
                    ParamPrefixEnum prefix = date.getPrefix() == null ? ParamPrefixEnum.EQUAL : date.getPrefix();
                    switch (prefix) {
                        case APPROXIMATE:
                            throw new NotImplementedOperationException("Approximate for date searches not implemented");
                        case ENDS_BEFORE:
                        case LESSTHAN:
                            subAndQuery.add(
                                TermRangeQuery.newStringRange(fieldName, null, date.getValueAsString(), true, false),
                                Occur.SHOULD);
                            break;
                        case EQUAL:
                            subAndQuery.add(new TermQuery(new Term(fieldName, date.getValueAsString())), Occur.SHOULD);
                            break;
                        case GREATERTHAN:
                        case STARTS_AFTER:
                            subAndQuery.add(
                                TermRangeQuery.newStringRange(fieldName, date.getValueAsString(), null, false, true),
                                Occur.SHOULD);
                            break;
                        case GREATERTHAN_OR_EQUALS:
                            subAndQuery.add(
                                TermRangeQuery.newStringRange(fieldName, date.getValueAsString(), null, true, true),
                                Occur.SHOULD);
                            break;
                        case LESSTHAN_OR_EQUALS:
                            subAndQuery.add(
                                TermRangeQuery.newStringRange(fieldName, null, date.getValueAsString(), true, true),
                                Occur.SHOULD);
                            break;
                        case NOT_EQUAL:
                            subAndQuery.add(new TermQuery(new Term(fieldName, date.getValueAsString())),
                                Occur.MUST_NOT);
                            break;
                        default:
                            throw new RuntimeException("Unknown DateParam prefix " + date.getPrefix());
                    }

                }
                builder.add(subAndQuery.build(), Occur.MUST);
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

    public static <T extends BaseResource> Builder createTextSearchBuilder(Class<T> clazz, StringAndListParam text,
            StringOrListParam status, DateAndListParam lastModified) {
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

        addOptionalDateAndList(lastModified, builder, FieldNames.LAST_MODIFIED);

        if (status == null || status.getValuesAsQueryTokens().size() == 0) {
            status = new StringOrListParam();
            status.add(new StringParam("active"));
        }

        QueryBuilder.addOptionalStringOrList(status, builder, FieldNames.STATUS);

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
