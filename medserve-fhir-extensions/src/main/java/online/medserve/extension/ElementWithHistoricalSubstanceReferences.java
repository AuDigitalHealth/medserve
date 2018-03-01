package online.medserve.extension;

import java.util.List;

public interface ElementWithHistoricalSubstanceReferences {

    List<ExtendedSubstanceReference> getIsReplacedByResources();

    List<ExtendedSubstanceReference> getReplacesResources();

}