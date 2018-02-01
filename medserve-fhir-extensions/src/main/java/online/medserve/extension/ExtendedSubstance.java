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

@ResourceDef(name = "Substance", id = "extendedSubstance", profile = ExtendedSubstance.PROFILE_URL_BASE
        + "Profile/ExtendedMedication")
public class ExtendedSubstance extends Substance implements IBaseResource, ResourceWithHistoricalAssociations {

    public static final String PROFILE_URL_BASE = "http://medserve.online/fhir/";
    private static final long serialVersionUID = 1L;

    /**
     * The date the underlying definition of the concept or its descriptions were last changed.
     */
    @Child(name = "lastModified", min = 1, max = 1, summary = false)
    @Extension(url = PROFILE_URL_BASE + "StructureDefinition/lastModified", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "The date the underlying definition of the concept or its descriptions were last changed.")
    private DateType lastModified;

    /**
     * Replacement resources for this resource
     */
    @Child(name = "replacementResources", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/replacementResources", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "Replacement resources for this resource")
    private List<ResourceReplacementExtension> replacementResources;

    /**
     * Resources that this resource has replaced
     */
    @Child(name = "replacedResources", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/replacedResources", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "Resources that this resource has replaced")
    private List<ResourceReplacedExtension> replacedResources;

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && ElementUtil.isEmpty(lastModified, replacementResources, replacedResources);
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

    /*
     * (non-Javadoc)
     * 
     * @see online.medserve.extension.ResourceWithHistoricalAssociations#getReplacementResources()
     */
    @Override
    public List<ResourceReplacementExtension> getReplacementResources() {
        if (replacementResources == null) {
            replacementResources = new ArrayList<>();
        }
        return replacementResources;
    }

    /*
     * (non-Javadoc)
     * 
     * @see online.medserve.extension.ResourceWithHistoricalAssociations#setReplacementResources(java.util.List)
     */
    @Override
    public void setReplacementResources(List<ResourceReplacementExtension> replacementResources) {
        this.replacementResources = replacementResources;
    }

    /*
     * (non-Javadoc)
     * 
     * @see online.medserve.extension.ResourceWithHistoricalAssociations#getReplacedResources()
     */
    @Override
    public List<ResourceReplacedExtension> getReplacedResources() {
        if (replacedResources == null) {
            replacedResources = new ArrayList<>();
        }
        return replacedResources;
    }

    /*
     * (non-Javadoc)
     * 
     * @see online.medserve.extension.ResourceWithHistoricalAssociations#setReplacedResources(java.util.List)
     */
    @Override
    public void setReplacedResources(List<ResourceReplacedExtension> replacedResources) {
        this.replacedResources = replacedResources;
    }
}
