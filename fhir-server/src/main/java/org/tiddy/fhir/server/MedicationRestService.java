package org.tiddy.fhir.server;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import ca.uhn.fhir.model.dstu2.resource.Medication;

@Path("/Medication")
public class MedicationRestService {

    @Inject
    MedicationStore store;

    @GET
    @Path("/getAll")
    @Produces({ "application/json" })
    public List<Medication> getAll() {
        return Arrays.asList();
    }

    @GET
    @Path("/_search")
    @Produces({ "application/json" })
    public List<String> search(@QueryParam("_id") String id,
            @QueryParam("name") String name) {
        if (id != null) {
            return store.searchMedicationById(id);
        } else {
            return store.searchMedicationByName(name);
        }
    }

    @PUT
    @Path("/")
    @Produces({ "application/json" })
    public void create(String m) {
        store.addMedication(m);
    }

}
