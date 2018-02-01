package online.medserve.server.bundleprovider;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import online.medserve.server.Util;
import online.medserve.server.index.Index;

public class TextSearchBundleProvider implements IBundleProvider {
    private InstantDt searchTime;
    private Integer pageSize;
    private StringAndListParam text;
    private Index index;
    private Class<? extends BaseResource> clazz;
    private int size;
    private StringOrListParam status;
    private DateAndListParam lastModified;

    public TextSearchBundleProvider(Class<? extends BaseResource> clazz, Index index, StringAndListParam text,
            StringOrListParam status, DateAndListParam lastModified, Integer pageSize) throws IOException {
        searchTime = InstantDt.withCurrentTime();
        this.clazz = clazz;
        this.index = index;
        this.text = text;
        this.status = status;
        this.lastModified = lastModified;
        this.pageSize = Util.getCount(pageSize);
        
        this.size = index.getResourcesByTextSize(clazz, text, status, lastModified);
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
        return index.getResourcesByText(clazz, text, status, lastModified, theFromIndex, theToIndex);
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
