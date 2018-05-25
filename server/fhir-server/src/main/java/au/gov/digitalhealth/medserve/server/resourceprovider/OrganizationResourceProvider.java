package au.gov.digitalhealth.medserve.server.resourceprovider;

import java.io.IOException;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.instance.model.api.IBaseResource;

import au.gov.digitalhealth.medserve.server.bundleprovider.TextSearchBundleProvider;
import au.gov.digitalhealth.medserve.server.index.Index;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;

public class OrganizationResourceProvider implements IResourceProvider {
    private Index index;

    public OrganizationResourceProvider(Index index) {
        this.index = index;
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Organization.class;
    }

    @Read()
    public Organization getResourceById(@IdParam IdType theId) throws IOException {
        return index.getResourceById(Organization.class, theId.getIdPart());
    }

    @Search()
    public IBundleProvider searchByText(
            @OptionalParam(name = "_text") @Description(shortDefinition = "Search of the resource narrative") StringAndListParam text,
            @Count Integer theCount) throws IOException {

        return new TextSearchBundleProvider(Organization.class, index, null, text, null, null, theCount);
    }
}
