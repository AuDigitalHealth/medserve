package org.tiddy.fhir.server;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/Substance")
public class SubstanceRestService {

    @Inject
    MedicationStore store;

    @PUT
    @Path("/")
    @Produces({ "application/json" })
    public void create(String s) {
        store.addSubstance(s);
    }

    @GET
    @Path("/_search")
    @Produces({ "application/json" })
    public List<String> search(@QueryParam("_id") String id) {
        return store.searchSubstanceById(id);
    }

}
