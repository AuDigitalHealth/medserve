package online.medserve.extension;

import java.util.List;

public interface ResourceWithHistoricalAssociations {

    List<IsReplacedByExtension> getReplacementResources();

    void setReplacementResources(List<IsReplacedByExtension> replacementResources);

    List<ReplacesResourceExtension> getReplacedResources();

    void setReplacedResources(List<ReplacesResourceExtension> replacedResources);

}