package au.gov.digitalhealth.medserve.server.resourceprovider;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.instance.model.api.IBaseResource;

import au.gov.digitalhealth.medserve.extension.ExtendedMedication;
import au.gov.digitalhealth.medserve.server.Util;
import au.gov.digitalhealth.medserve.server.index.Index;
import au.gov.digitalhealth.medserve.server.indexbuilder.constants.FieldNames;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.NumberAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;

public class MedicationResourceProvider implements IResourceProvider {
    private Index index;

    public MedicationResourceProvider(Index index) {
        this.index = index;
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Medication.class;
    }

    @Read(type = ExtendedMedication.class)
    public ExtendedMedication getResourceById(@IdParam IdType theId) throws IOException {
        return index.getResourceById(ExtendedMedication.class, theId.getIdPart());
    }

    @Search(type = ExtendedMedication.class)
    public IBundleProvider search(
            @OptionalParam(name = ExtendedMedication.SP_CODE) @Description(shortDefinition = "Search the resource's codings") TokenAndListParam code,
            @OptionalParam(name = "_text") @Description(shortDefinition = "Search of the resource narrative") StringAndListParam text,
            @OptionalParam(name = FieldNames.PARENT) @Description(shortDefinition = "Deprecated, use ancestor instead. Search for resources covered by but more specific than the specified abstract medication") TokenAndListParam parent,
            @OptionalParam(name = FieldNames.ANCESTOR) @Description(shortDefinition = "Search for resources covered by but more specific than the specified abstract medication") TokenAndListParam ancestor,
            @OptionalParam(name = FieldNames.MEDICATION_RESOURCE_TYPE) @Description(shortDefinition = "Search for resources with the specified resource type") StringOrListParam medicationResourceType,
            @OptionalParam(name = ExtendedMedication.SP_FORM) TokenAndListParam form,
            @OptionalParam(name = ExtendedMedication.SP_CONTAINER) TokenAndListParam container,
            @OptionalParam(name = ExtendedMedication.SP_INGREDIENT) TokenAndListParam ingredient,
            @OptionalParam(name = ExtendedMedication.SP_PACKAGE_ITEM) TokenAndListParam packageItem,
            @OptionalParam(name = FieldNames.BRAND) @Description(shortDefinition = "Search for resources with the specified coded brand") TokenAndListParam brand,
            @OptionalParam(name = FieldNames.IS_BRAND) @Description(shortDefinition = "Filter true/false on whether to match branded or unbranded (generic) resources") String isBrand,
            @OptionalParam(name = FieldNames.MANUFACTURER) @Description(shortDefinition = "Search for resources with the specified manufacturer") TokenAndListParam manufacturer,
            @OptionalParam(name = FieldNames.SUBSIDY_CODE) @Description(shortDefinition = "Search for resources with the specified subsidy code") TokenAndListParam subsidyCode,
            @OptionalParam(name = ExtendedMedication.SP_STATUS) @Description(shortDefinition = "Status of the medication, active, inactive (meaning no longer available) or entered-in-error") StringOrListParam status,
            @OptionalParam(name = FieldNames.LAST_MODIFIED) @Description(shortDefinition = "Date the underlying code system's content for this medication was last modified") DateAndListParam lastModified,
            @OptionalParam(name = FieldNames.INGREDIENT_COUNT) @Description(shortDefinition = "Filter on the number of ingredients a Medication has") NumberAndListParam ingredientCount,
            @Count Integer theCount) throws IOException {
        final InstantDt searchTime = InstantDt.withCurrentTime();
        final int size = index.getMedicationsByParametersSize(ExtendedMedication.class, code, text, parent, ancestor,
            medicationResourceType, form, container, ingredient, packageItem, brand, isBrand, manufacturer,
            subsidyCode, status, lastModified, ingredientCount);

        return new IBundleProvider() {

            @Override
            public Integer size() {
                return size;
            }

            @Override
            public List<IBaseResource> getResources(int theFromIndex, int theToIndex) {
                if (theFromIndex >= size) {
                    return Collections.emptyList();
                }
                return index.getMedicationsByParameters(ExtendedMedication.class, code, text, parent, ancestor,
                    medicationResourceType, form, container, ingredient, packageItem, brand, isBrand, manufacturer,
                    subsidyCode, status, lastModified, ingredientCount, theFromIndex, theToIndex);
            }

            @Override
            public InstantDt getPublished() {
                return searchTime;
            }

            @Override
            public Integer preferredPageSize() {
                return Util.getCount(theCount);
            }

            @Override
            public String getUuid() {
                return null;
            }
        };
    }


}
