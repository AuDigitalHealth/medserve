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
public class IsReplacedByExtension extends BackboneElement {

    private static final long serialVersionUID = 1L;

    /**
     * A reference to a replacement resource
     */
    @Child(name = "isReplacedBy", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/isReplacedBy", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "A reference to a replacement resource")
    private Reference isReplacedBy;

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

    public IsReplacedByExtension() {
        super();
    }

    public IsReplacedByExtension(Reference resourceReplacement, Coding replacementType,
            DateType replacementDate) {
        super();
        this.isReplacedBy = resourceReplacement;
        this.replacementType = replacementType;
        this.replacementDate = replacementDate;
    }

    /**
     * It is important to override the isEmpty() method, adding a check for any newly added fields.
     */
    @Override
    public boolean isEmpty() {
        return super.isEmpty()
                && ElementUtil.isEmpty(isReplacedBy, replacementType, replacementDate);
    }

    @Override
    public IsReplacedByExtension copy() {
        IsReplacedByExtension copy = new IsReplacedByExtension();
        copy.isReplacedBy = isReplacedBy;
        copy.replacementType = replacementType;
        copy.replacementDate = replacementDate;
        return copy;
    }

    public Reference getResourceReplacement() {
        return isReplacedBy;
    }

    public void setResourceReplacement(Reference resourceReplacement) {
        this.isReplacedBy = resourceReplacement;
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
