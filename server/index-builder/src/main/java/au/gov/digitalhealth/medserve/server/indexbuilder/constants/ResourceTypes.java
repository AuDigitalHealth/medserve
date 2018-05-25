package au.gov.digitalhealth.medserve.server.indexbuilder.constants;

public final class ResourceTypes {

    public static final String MEDICATION_RESOURCE_TYPE_VALUE = "Medication";
    public static final String ORGANIZATION_RESOURCE_TYPE_VALUE = "Organization";
    public static final String SUBSTANCE_RESOURCE_TYPE_VALUE = "Substance";

    private ResourceTypes() {
        throw new AssertionError("Constants class not to be constructed!");
    }
}
