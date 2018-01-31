package online.medserve.server.resourceprovider;

import java.io.IOException;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import online.medserve.server.bundleprovider.TextSearchBundleProvider;
import online.medserve.server.index.Index;

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
            @RequiredParam(name = "_text") @Description(shortDefinition = "Search of the resource narrative") StringAndListParam text,
            @Count Integer theCount) throws IOException {

        return new TextSearchBundleProvider(Organization.class, index, text, null, theCount);
    }
}
