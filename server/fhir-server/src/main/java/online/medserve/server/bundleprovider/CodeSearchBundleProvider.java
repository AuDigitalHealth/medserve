package online.medserve.server.bundleprovider;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IBundleProvider;
import online.medserve.server.Util;
import online.medserve.server.index.Index;

public class CodeSearchBundleProvider implements IBundleProvider {
    private InstantDt searchTime;
    private Integer pageSize;
    private TokenParam code;
    private Index index;
    private Class<? extends BaseResource> clazz;
    private int size;

    public CodeSearchBundleProvider(Class<? extends BaseResource> clazz, Index index, TokenParam code,
            Integer pageSize) throws IOException {
        searchTime = InstantDt.withCurrentTime();
        this.clazz = clazz;
        this.index = index;
        this.code = code;
        this.pageSize = Util.getCount(pageSize);
        
        this.size = index.getResourcesByCodeSize(clazz, code);
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
        return index.getResourcesByCode(clazz, code, theFromIndex, theToIndex);
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
    public int size() {
        return size;
    }

}
