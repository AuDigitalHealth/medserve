package au.gov.digitalhealth.medserve.extension;

import java.util.HashMap;

import org.hl7.fhir.dstu3.model.Coding;

public enum MedicationType {
    UnbrandedProduct("UPD", "Unbranded product with no strengths or form"),
    UnbrandedProductStrength("UPDS", "Unbranded product with strengths but no form"),
    UnbrandedProductForm("UPDF", "Unbranded product with form but no strength"),
    UnbrandedProductStrengthForm("UPDSF", "Unbranded product with strengths and form"),
    UnbrandedPackage("UPG", "Unbranded package with no container"),
    BrandedProduct("BPD", "Branded product without strengths or form"),
    BrandedProductStrength("BPDS", "Branded product with strengths but no form"),
    BrandedProductForm("BPDF", "Branded product with form but no strengths"),
    BrandedProductStrengthForm("BPSF", "Branded product with strengths and form"),
    BrandedPackage("BPG", "Branded package with no container"),
    BrandedPackgeContainer("BPGC", "Branded package with container");

    private Coding code;

    private static HashMap<String, MedicationType> instanceMap = new HashMap<>();

    static {
        for (MedicationType instance : MedicationType.values()) {
            instanceMap.put(instance.code.getCode(), instance);
        }
    }

    public static MedicationType fromCode(String code) {
        if (instanceMap.containsKey(code)) {
            return instanceMap.get(code);
        } else {
            throw new RuntimeException("Cannot find enum for code " + code);
        }
    }

    MedicationType(String code, String display) {
        this.code = new Coding("http://hl7.org/fhir/medication_type", code, display);
    }

    public Coding getCode() {
        return code;
    }

    public boolean isBranded() {
        return this.name().startsWith("B");
    }

}
