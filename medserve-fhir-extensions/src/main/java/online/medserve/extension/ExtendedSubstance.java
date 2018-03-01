package online.medserve.extension;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.Substance;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.api.annotation.Extension;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.util.ElementUtil;

@ResourceDef(name = "ExtendedSubstance", id = "extendedSubstance", profile = ExtendedMedication.PROFILE_URL_BASE
        + "Profile/ExtendedSubstance")
public class ExtendedSubstance extends Substance implements IBaseResource, ElementWithHistoricalSubstanceReferences {

    private static final long serialVersionUID = 1L;

    /**
     * The date the underlying definition of the concept or its descriptions were last changed.
     */
    @Child(name = "lastModified", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/lastModified", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "The date the underlying definition of the concept or its descriptions were last changed.")
    private DateType lastModified;

    /**
     * Replacement resources for this resource
     */
    @Child(name = "isReplacedByResources", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/isReplacedByResources", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "Replacement resources for this resource")
    private List<ExtendedSubstanceReference> isReplacedByResources;

    /**
     * Resources that this resource has replaced
     */
    @Child(name = "replacesResources", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/replacesResources", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "Resources that this resource has replaced")
    private List<ExtendedSubstanceReference> replacesResources;

    /**
     * The code system that was the source of this {@link ExtendedMedication} resource.
     */
    @Child(name = "sourceCodeSystem", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/sourceCodeSystem", definedLocally = true, isModifier = false)
    @Description(shortDefinition = "The code system that was the source of this {@link ExtendedMedication} resource.")
    private SourceCodeSystemExtension sourceCodeSystem;

    @Override
    public boolean isEmpty() {
        return super.isEmpty()
                && ElementUtil.isEmpty(lastModified, isReplacedByResources, replacesResources, sourceCodeSystem);
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

    public SourceCodeSystemExtension getSourceCodeSystem() {
        return sourceCodeSystem;
    }

    public void setSourceCodeSystem(SourceCodeSystemExtension sourceCodeSystem) {
        this.sourceCodeSystem = sourceCodeSystem;
    }

    /* (non-Javadoc)
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

    /* (non-Javadoc)
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

}
