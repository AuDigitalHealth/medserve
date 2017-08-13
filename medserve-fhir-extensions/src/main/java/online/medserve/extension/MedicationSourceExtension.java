package online.medserve.extension;

import org.hl7.fhir.dstu3.model.BackboneElement;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.UriType;

import ca.uhn.fhir.model.api.annotation.Block;
import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.api.annotation.Extension;
import ca.uhn.fhir.util.ElementUtil;

@Block
public class MedicationSourceExtension extends BackboneElement {

    private static final long serialVersionUID = 1L;

    /**
     * URI of the code system that was the source of this {@link ExtendedMedication} resource.
     */
    @Child(name = "uri", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/sourceCodeSystemUri", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "URI of the code system that was the source of this {@link ExtendedMedication} resource.")
    private UriType uri;

    /**
     * Version of the code system that was the source of this {@link ExtendedMedication} resource.
     */
    @Child(name = "version", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/sourceCodeSystemVersion", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "Version of the code system that was the source of this {@link ExtendedMedication} resource.")
    private StringType version;

    public MedicationSourceExtension() {
        super();
    }

    public MedicationSourceExtension(UriType uri, StringType version) {
        super();
        this.uri = uri;
        this.version = version;
    }

    /**
     * It is important to override the isEmpty() method, adding a check for any newly added fields.
     */
    @Override
    public boolean isEmpty() {
        return super.isEmpty()
                && ElementUtil.isEmpty(uri, version);
    }

    @Override
    public MedicationSourceExtension copy() {
        MedicationSourceExtension copy = new MedicationSourceExtension();
        copy.uri = uri;
        copy.version = version;
        return copy;
    }

}
