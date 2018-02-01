package online.medserve.extension;

import java.util.List;

public interface ResourceWithHistoricalAssociations {

    List<ResourceReplacementExtension> getReplacementResources();

    void setReplacementResources(List<ResourceReplacementExtension> replacementResources);

    List<ResourceReplacedExtension> getReplacedResources();

    void setReplacedResources(List<ResourceReplacedExtension> replacedResources);

}