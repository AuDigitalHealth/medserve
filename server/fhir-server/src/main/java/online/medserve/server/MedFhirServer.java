package online.medserve.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import ca.uhn.fhir.rest.server.EncodingEnum;
import ca.uhn.fhir.rest.server.FifoMemoryPagingProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import online.medserve.server.index.Index;
import online.medserve.server.resourceprovider.MedicationResourceProvider;
import online.medserve.server.resourceprovider.OrganizationResourceProvider;
import online.medserve.server.resourceprovider.SubstanceResourceProvider;

@WebServlet(urlPatterns = { "/fhir/*" }, displayName = "FHIR Server")
public class MedFhirServer extends RestfulServer {
    private static final long serialVersionUID = 1L;

    private static FifoMemoryPagingProvider pp = new FifoMemoryPagingProvider(100);

    /**
     * The initialize method is automatically called when the servlet is starting up, so it can
     * be used to configure the servlet to define resource providers, or set up
     * configuration, interceptors, etc.
     */
    @Override
    protected void initialize() throws ServletException {
        /*
         * The servlet defines any number of resource providers, and
         * configures itself to use them by calling
         * setResourceProviders()
         */
        List<IResourceProvider> resourceProviders = new ArrayList<IResourceProvider>();
        Index index;
        try {
            index = new Index();
        } catch (IOException e) {
            throw new ServletException("Could not create index for " + Index.INDEX_LOCATION);
        }
        resourceProviders.add(new MedicationResourceProvider(index));
        resourceProviders.add(new SubstanceResourceProvider(index));
        resourceProviders.add(new OrganizationResourceProvider(index));
        resourceProviders.add()
        setResourceProviders(resourceProviders);
        setDefaultResponseEncoding(EncodingEnum.JSON);

        pp.setDefaultPageSize(10);
        pp.setMaximumPageSize(100);
        setPagingProvider(pp);
    }
}
