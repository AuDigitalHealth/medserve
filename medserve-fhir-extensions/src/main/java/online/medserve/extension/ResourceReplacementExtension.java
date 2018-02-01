package online.medserve.extension;

import org.hl7.fhir.dstu3.model.BackboneElement;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.Reference;

import ca.uhn.fhir.model.api.annotation.Block;
import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.api.annotation.Extension;
import ca.uhn.fhir.util.ElementUtil;

@Block
public class ResourceReplacementExtension extends BackboneElement {

    private static final long serialVersionUID = 1L;

    /**
     * A reference to a replacement resource
     */
    @Child(name = "resourceReplacement", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/resourceReplacement", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "A reference to a replacement resource")
    private Reference resourceReplacement;

    /**
     * Replacement type
     */
    @Child(name = "replacementType", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/replacementType", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "Replacement type. Some types are a one for one replacement, others can be one to many")
    private Coding replacementType;

    /**
     * Replacement date
     */
    @Child(name = "replacementDate", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/replacementDate", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "Date the replacement was declared")
    private DateType replacementDate;

    public ResourceReplacementExtension() {
        super();
    }

    public ResourceReplacementExtension(Reference resourceReplacement, Coding replacementType,
            DateType replacementDate) {
        super();
        this.resourceReplacement = resourceReplacement;
        this.replacementType = replacementType;
        this.replacementDate = replacementDate;
    }

    /**
     * It is important to override the isEmpty() method, adding a check for any newly added fields.
     */
    @Override
    public boolean isEmpty() {
        return super.isEmpty()
                && ElementUtil.isEmpty(resourceReplacement, replacementType, replacementDate);
    }

    @Override
    public ResourceReplacementExtension copy() {
        ResourceReplacementExtension copy = new ResourceReplacementExtension();
        copy.resourceReplacement = resourceReplacement;
        copy.replacementType = replacementType;
        copy.replacementDate = replacementDate;
        return copy;
    }

    public Reference getResourceReplacement() {
        return resourceReplacement;
    }

    public void setResourceReplacement(Reference resourceReplacement) {
        this.resourceReplacement = resourceReplacement;
    }

    public Coding getReplacementType() {
        return replacementType;
    }

    public void setReplacementType(Coding replacementType) {
        this.replacementType = replacementType;
    }

    public DateType getReplacementDate() {
        return replacementDate;
    }

    public void setReplacementDate(DateType replacementDate) {
        this.replacementDate = replacementDate;
    }

}
