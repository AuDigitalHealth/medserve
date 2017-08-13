package online.medserve.transform.rxnorm.enumeration;

import java.util.HashMap;

public enum RxNormReltaionshipType {
    IncludedIn("included_in"),
    Includes("includes"),
    HasTradename("has_tradename"),
    HasIngredient("has_ingredient"),
    PreciseIngredientOf("precise_ingredient_of"),
    TradenameOf("tradename_of"),
    HasPart("has_part"),
    FormOf("form_of"),
    HasPreciseIngredient("has_precise_ingredient"),
    HasForm("has_form"),
    ReformulationOf("reformulation_of"),
    HasDoseForm("has_dose_form"),
    InverseIsa("inverse_isa"),
    PartOf("part_of"),
    HasIngredients("has_ingredients"),
    IngredientOf("ingredient_of"),
    DoseFormOf("dose_form_of"),
    Isa("isa"),
    Constitutes("constitutes"),
    IngredientsOf("ingredients_of"),
    Contains("contains"),
    ConsistsOf("consists_of"),
    QuantifiedFormOf("quantified_form_of"),
    HasQuantifiedForm("has_quantified_form"),
    ReformulatedTo("reformulated_to"),
    ContainedIn("contained_in"),
    HasDoseFormGroup("has_doseformgroup"),
    DoseFormGroupOf("doseformgroup_of");

    private static HashMap<String, RxNormReltaionshipType> instanceMap = new HashMap<>();

    static {
        for (RxNormReltaionshipType instance : RxNormReltaionshipType.values()) {
            instanceMap.put(instance.code, instance);
        }
    }

    public static boolean isEnumValue(String code) {
        return instanceMap.containsKey(code);
    }

    public static RxNormReltaionshipType fromCode(String code) {
        if (instanceMap.containsKey(code)) {
            return instanceMap.get(code);
        } else {
            throw new RuntimeException("Cannot find RxNormReltaionshipType for code " + code);
        }
    }

    private String code;

    private RxNormReltaionshipType(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
}
