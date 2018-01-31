package online.medserve.server.resourceprovider;

import java.io.IOException;

import org.apache.lucene.queryparser.classic.ParseException;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Substance;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import online.medserve.server.bundleprovider.CodeSearchBundleProvider;
import online.medserve.server.bundleprovider.TextSearchBundleProvider;
import online.medserve.server.index.Index;

public class SubstanceResourceProvider implements IResourceProvider {
    private Index index;

    public SubstanceResourceProvider(Index index) {
        this.index = index;
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Substance.class;
    }

    @Read()
    public Substance getResourceById(@IdParam IdType theId) throws IOException {
        return index.getResourceById(Substance.class, theId.getIdPart());
    }

    @Search(type = Substance.class)
    public IBundleProvider searchByCode(
            @RequiredParam(name = Substance.SP_CODE) TokenParam code, @Count Integer theCount)
            throws ParseException, IOException {
        return new CodeSearchBundleProvider(Substance.class, index, code, theCount);
    }

    @Search(type = Substance.class)
    public IBundleProvider searchByText(
            @RequiredParam(name = "_text") @Description(shortDefinition = "Search of the resource narrative") StringAndListParam text,
            @OptionalParam(name = Substance.SP_STATUS) @Description(shortDefinition = "Status of the substance, active, inactive (meaning no longer available) or entered-in-error") StringOrListParam status,
            @Count Integer theCount) throws IOException {

        return new TextSearchBundleProvider(Substance.class, index, text, status, theCount);
    }
}
