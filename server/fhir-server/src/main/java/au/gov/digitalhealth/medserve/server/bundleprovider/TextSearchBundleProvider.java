package au.gov.digitalhealth.medserve.server.bundleprovider;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import au.gov.digitalhealth.medserve.server.Util;
import au.gov.digitalhealth.medserve.server.index.Index;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;

public class TextSearchBundleProvider implements IBundleProvider {
    private InstantDt searchTime;
    private Integer pageSize;
    private StringAndListParam text;
    private Index index;
    private Class<? extends BaseResource> clazz;
    private int size;
    private StringOrListParam status;
    private DateAndListParam lastModified;
    private TokenAndListParam code;

    public TextSearchBundleProvider(Class<? extends BaseResource> clazz, Index index, TokenAndListParam code,
            StringAndListParam text, StringOrListParam status, DateAndListParam lastModified, Integer pageSize)
            throws IOException {
        searchTime = InstantDt.withCurrentTime();
        this.clazz = clazz;
        this.index = index;
        this.text = text;
        this.status = status;
        this.lastModified = lastModified;
        this.pageSize = Util.getCount(pageSize);
        this.code = code;
        
        this.size = index.getResourcesByTextSize(clazz, code, text, status, lastModified);
    }

    @Override
    public IPrimitiveType<Date> getPublished() {
        return searchTime;
    }

    @Override
    public List<IBaseResource> getResources(int theFromIndex, int theToIndex) {
        if (theFromIndex >= size) {
            return Collections.emptyList();
        }
        return index.getResourcesByText(clazz, code, text, status, lastModified, theFromIndex, theToIndex);
    }

    @Override
    public String getUuid() {
        return null;
    }

    @Override
    public Integer preferredPageSize() {
        return pageSize;
    }

    @Override
    public Integer size() {
        return size;
    }

}
