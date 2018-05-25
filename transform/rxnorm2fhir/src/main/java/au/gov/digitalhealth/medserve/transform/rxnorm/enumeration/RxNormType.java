package au.gov.digitalhealth.medserve.transform.rxnorm.enumeration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import au.gov.digitalhealth.medserve.extension.MedicationType;

public enum RxNormType {
    BrandName("BN", MedicationType.BrandedProduct, "Fully-specified drug brand name that can not be prescribed"),
    BrandedPack("BPCK", MedicationType.BrandedPackage, "Branded Drug Delivery Device"),
    DoseFormGroup("DFG", null, "Dose Form Group"),
    DoseForm("DF", null, "Dose Form"),
    GenericPack("GPCK", MedicationType.UnbrandedPackage, "Generic Drug Delivery Device"),
    Ingredient("IN", null, "Name for an ingredient"),
//    MultiIngredient("MIN", null, "name for a multi-ingredient"),
    PreciseIngredient("PIN", null, "Name from a precise ingredient"),
    PrescribableName("PSN", null, "Prescribable Names"),
    SemanticBrandedDrugComponent("SBDC", MedicationType.BrandedProductStrength, "Semantic Branded Drug Component - Ingredient + Strength + Brand Name"),
    SemanticBrandedDrugAndForm("SBDF", MedicationType.BrandedProductForm, "Semantic branded drug and form - Ingredient + Dose Form + Brand Name"),
    // SemanticBrandedDrugGroup("SBDG", MedicationType.BrandedProductFormNoStrength, "Semantic branded drug group -
    // Brand Name + Dose Form Group"),
    SemanticBrandedDrug("SBD", MedicationType.BrandedProductStrengthForm, "Semantic branded drug - Ingredient + Strength + Dose Form + Brand Name"),
    SemanticDrugComponent("SCDC", MedicationType.UnbrandedProductStrength, "Semantic Drug Component - Ingredient + Strength"),
    SemanticClinicalDrugAndForm("SCDF", MedicationType.UnbrandedProductForm, "Semantic clinical drug and form - Ingredient + Dose Form"),
    // SemanticClinicalDrugGroup("SCDG", "Semantic clinical drug group - Ingredient + Dose Form Group"),
    SemanticClinicalDrug("SCD", MedicationType.UnbrandedProductStrengthForm, "Semantic Clinical Drug - Ingredient + Strength + Dose Form"),
    FullFormOfDescriptor("FN", null, "Full form of descriptor"),
    ActiveSubstance("SU", null, "Active Substance"), // UNII
    TallManSynonym("TMSY", null, "Tall Man synonym");

    private static Set<RxNormType> baseTypes = new HashSet<>();

    private static HashMap<String, RxNormType> instanceMap = new HashMap<>();

    static {
        for (RxNormType instance : RxNormType.values()) {
            instanceMap.put(instance.code, instance);
        }
    }

    public static RxNormType fromCode(String code) {
        if (instanceMap.containsKey(code)) {
            return instanceMap.get(code);
        } else {
            throw new RuntimeException("Cannot find RxNormType for code " + code);
        }
    }

    public static boolean isEnumValue(String code) {
        return instanceMap.containsKey(code);
    }

    private String code, description;

    private MedicationType medicationType;

    public MedicationType getMedicationType() {
        return medicationType;
    }

    private RxNormType(String code, MedicationType medicationType, String description) {
        this.code = code;
        this.description = description;
        this.medicationType = medicationType;
    }
    
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isBaseType() {
        switch (this) {
            case BrandName:
            case BrandedPack:
            case GenericPack:
            case Ingredient:
            case PreciseIngredient:
            case SemanticBrandedDrugComponent:
            case SemanticBrandedDrugAndForm:
            case SemanticBrandedDrug:
            case SemanticDrugComponent:
            case SemanticClinicalDrugAndForm:
            case SemanticClinicalDrug:
                return true;

            default:
                return false;
        }
    }
}
