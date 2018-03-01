package online.medserve.extension;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.Enumeration;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.codesystems.SubstanceStatus;
import org.hl7.fhir.instance.model.api.ICompositeType;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.api.annotation.Extension;
import ca.uhn.fhir.util.ElementUtil;

@DatatypeDef(name = "ExtendedSubstanceReference", isSpecialization = true)
public class ExtendedSubstanceReference extends Reference
        implements ICompositeType, ElementWithHistoricalSubstanceReferences {

    public static final String PROFILE_URL_BASE = "http://medserve.online/fhir/";
    private static final long serialVersionUID = 1L;

    /**
     * Status of the referenced Substance
     */
    @Child(name = "status", min = 1, max = 1, summary = true)
    @Extension(url = PROFILE_URL_BASE
            + "StructureDefinition/ExtendedSubstanceReference/status", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "Status of the referenced Substance")
    private Enumeration<SubstanceStatus> status;

    /**
     * The date the underlying definition of the concept or its descriptions were last changed.
     */
    @Child(name = "lastModified", min = 1, max = 1, summary = false)
    @Extension(url = PROFILE_URL_BASE
            + "StructureDefinition/lastModified", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "The date the underlying definition of the concept or its descriptions were last changed.")
    private DateType lastModified;

    /**
     * A reference to a resource the resource replaces or is replaced by - this is contextual depending upon where this
     * reference appears
     */
    @Child(name = "historicallyRelatedResource", min = 0, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/ExtendedReference/historicallyRelatedResource", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "A reference to a resource the resource replaces or is replaced by - this is contextual depending upon where this reference appears")
    private ExtendedSubstanceReference historicallyRelatedResource;

    /**
     * The date this resource replaced, or was replaced by, the historicallyRelatedResource.
     */
    @Child(name = "replacementDate", min = 0, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/ExtendedReference/replacementDate", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "The date this resource replaced, or was replaced by, the historicallyRelatedResource.")
    private DateType replacementDate;

    /**
     * The type of replacement
     */
    @Child(name = "replacementType", min = 0, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/ExtendedReference/replacementType", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "The type of the replacement")
    private Coding replacementType;

    /**
     * True if the referenced resource replaces the historicallyRelatedResource or false if it is replaced by the
     * historicallyRelatedResource
     */
    @Child(name = "replacesHistoricallyRelatedResource", min = 0, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/ExtendedReference/replacesHistoricallyRelatedResource", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "True if the referenced resource replaces the historicallyRelatedResource or false if it is replaced by the historicallyRelatedResource")
    private BooleanType replacesHistoricallyRelatedResource;

    /**
     * Replacement resources for this resource
     */
    @Child(name = "isReplacedByResources", min = 0, max = Child.MAX_UNLIMITED, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/isReplacedByResources", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "Replacement resources for this resource")
    private List<ExtendedSubstanceReference> isReplacedByResources;

    /**
     * Resources that this resource has replaced
     */
    @Child(name = "replacesResources", min = 0, max = Child.MAX_UNLIMITED, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/replacesResources", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "Resources that this resource has replaced")
    private List<ExtendedSubstanceReference> replacesResources;

    public ExtendedSubstanceReference() {}

    public ExtendedSubstanceReference(String theReference) {
        super(theReference);
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && ElementUtil.isEmpty(status, lastModified, historicallyRelatedResource,
            replacementDate, replacesHistoricallyRelatedResource, isReplacedByResources, replacesResources);
    }


    public Enumeration<SubstanceStatus> getStatus() {
        return status;
    }

    public void setStatus(Enumeration<SubstanceStatus> status) {
        this.status = status;
    }

    public DateType getLastModified() {
        if (lastModified == null) {
            lastModified = new DateType();
        }
        return lastModified;
    }

    public void setLastModified(DateType lastModified) {
        this.lastModified = lastModified;
    }

    public ExtendedSubstanceReference getHistoricallyRelatedResource() {
        return historicallyRelatedResource;
    }

    public void setHistoricallyRelatedResource(ExtendedSubstanceReference historicallyRelatedResource) {
        this.historicallyRelatedResource = historicallyRelatedResource;
    }

    public DateType getReplacementDate() {
        return replacementDate;
    }

    public void setReplacementDate(DateType replacementDate) {
        this.replacementDate = replacementDate;
    }

    public BooleanType getReplacesHistoricallyRelatedResource() {
        return replacesHistoricallyRelatedResource;
    }

    public void setReplacesHistoricallyRelatedResource(BooleanType replacesHistoricallyRelatedResource) {
        this.replacesHistoricallyRelatedResource = replacesHistoricallyRelatedResource;
    }

    /*
     * (non-Javadoc)
     * 
     * @see online.medserve.extension.ElementWithHistoricalSubstanceReferences#getIsReplacedByResources()
     */
    @Override
    public List<ExtendedSubstanceReference> getIsReplacedByResources() {
        if (isReplacedByResources == null) {
            isReplacedByResources = new ArrayList<>();
        }
        return isReplacedByResources;
    }

    public void setIsReplacedByResources(List<ExtendedSubstanceReference> isReplacedByResources) {
        this.isReplacedByResources = isReplacedByResources;
    }

    /*
     * (non-Javadoc)
     * 
     * @see online.medserve.extension.ElementWithHistoricalSubstanceReferences#getReplacesResources()
     */
    @Override
    public List<ExtendedSubstanceReference> getReplacesResources() {
        if (replacesResources == null) {
            replacesResources = new ArrayList<>();
        }
        return replacesResources;
    }

    public void setReplacesResources(List<ExtendedSubstanceReference> replacesResources) {
        this.replacesResources = replacesResources;
    }

    public Coding getReplacementType() {
        return replacementType;
    }

    public void setReplacementType(Coding replacementType) {
        this.replacementType = replacementType;
    }

}
