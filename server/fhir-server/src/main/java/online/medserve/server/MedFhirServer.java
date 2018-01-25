package online.medserve.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import ca.uhn.fhir.rest.api.EncodingEnum;
import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.fhir.rest.server.FifoMemoryPagingProvider;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import online.medserve.server.index.Index;
import online.medserve.server.resourceprovider.MedicationResourceProvider;
import online.medserve.server.resourceprovider.OrganizationResourceProvider;
import online.medserve.server.resourceprovider.SubstanceResourceProvider;

@WebServlet(urlPatterns = { "/fhir/*" }, displayName = "FHIR Server")
public class MedFhirServer extends RestfulServer {
    private static final long serialVersionUID = 1L;

    private String baseUrl = System.getenv("MEDSERVE_FHIR_BASE");

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
        setResourceProviders(resourceProviders);
        setDefaultResponseEncoding(EncodingEnum.JSON);

        pp.setDefaultPageSize(10);
        pp.setMaximumPageSize(100);
        setPagingProvider(pp);

        // Define CORS configuration.
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedHeader("x-fhir-starter");
        config.addAllowedHeader("Origin");
        config.addAllowedHeader("Accept");
        config.addAllowedHeader("X-Requested-With");
        config.addAllowedHeader("Content-Type");

        config.addAllowedOrigin("*");

        config.addExposedHeader("Location");
        config.addExposedHeader("Content-Location");
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Create the CORS interceptor and register it.
        CorsInterceptor interceptor = new CorsInterceptor(config);
        registerInterceptor(interceptor);

        // Set base FHIR endpoint, based upon the `MEDSERVE_FHIR_BASE` configuration variable.
        if (baseUrl != null && baseUrl != "") {
            log("Setting server FHIR base to " + baseUrl);
            setServerAddressStrategy(new HardcodedServerAddressStrategy(baseUrl));
        }
    }
}
