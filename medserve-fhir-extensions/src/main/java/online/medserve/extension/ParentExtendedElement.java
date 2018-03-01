package online.medserve.extension;

import java.util.Collection;

public interface ParentExtendedElement {

    void addParentMedicationResource(ExtendedMedicationReference parentMedicationResource);

    public Collection<ExtendedMedicationReference> getParentMedicationResources();
}