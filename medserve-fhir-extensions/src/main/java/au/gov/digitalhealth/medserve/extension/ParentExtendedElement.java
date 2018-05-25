package au.gov.digitalhealth.medserve.extension;

import java.util.Collection;
import java.util.List;

public interface ParentExtendedElement {

    public Collection<MedicationParentExtension> getParentMedicationResources();

    public void setParentMedicationResources(List<MedicationParentExtension> parentMedicationResources);

    public void addParentMedicationResources(MedicationParentExtension parentMedicationResource);

}
