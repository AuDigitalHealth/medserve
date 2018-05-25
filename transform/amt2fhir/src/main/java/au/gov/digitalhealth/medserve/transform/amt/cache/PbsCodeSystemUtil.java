package au.gov.digitalhealth.medserve.transform.amt.cache;

public class PbsCodeSystemUtil {

    private PbsCodeSystemUtil() {
        throw new AssertionError("Static method class, should not be instantiated");
    }

    public static String getRestrictionCodeDisplay(String restriction) {
        switch (restriction) {
            case "U":
                return "Unrestricted";
            case "R":
                return "Restricted benefit";
            case "A":
                return "Authority required";

            default:
                throw new RuntimeException("No display for code " + restriction);
        }
    }

    public static String getProgramCodeDisplay(String programCode) {
        switch (programCode) {
            case "EP":
                return "Extemporaneous Preparations";
            case "GE":
                return "Generally Available Pharmaceutical Benefits";
            case "PL":
                return "Palliative Care";
            case "DB":
                return "Prescriber Bag";
            case "R1":
                return "Repatriation Pharmaceutical Benefits Scheme only";
            case "MF":
                return "Botulinum Toxin Program";
            case "IN":
                return "Efficient Funding of Chemotherapy - Private Hospital – infusibles";
            case "IP":
                return "Efficient Funding of Chemotherapy - Public Hospital - infusibles";
            case "CT":
                return "Efficient Funding of Chemotherapy - Related Benefits";
            case "TY":
                return "Efficient Funding of Chemotherapy - Private Hospital - Trastuzumab";
            case "TZ":
                return "Efficient Funding of Chemotherapy - Public Hospital - Trastuzumab";
            case "GH":
                return "Growth Hormone Program";
            case "HS":
                return "Highly Specialised Drugs Program - Private Hospital";
            case "HB":
                return "Highly Specialised Drugs Program - Public Hospital";
            case "CA":
                return "Highly Specialised Drugs Program - Community Access";
            case "IF":
                return "IVF Program";
            case "MD":
                return "Opiate Dependence Treatment Program";
            case "PQ":
                return "Paraplegic and Quadriplegic Program";

            default:
                throw new RuntimeException("No display for code " + programCode);
        }
    }
}
