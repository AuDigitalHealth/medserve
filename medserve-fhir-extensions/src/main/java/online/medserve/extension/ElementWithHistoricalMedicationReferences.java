package online.medserve.extension;

import java.util.List;

public interface ElementWithHistoricalMedicationReferences {

    List<ExtendedMedicationReference> getIsReplacedByResources();

    List<ExtendedMedicationReference> getReplacesResources();

}