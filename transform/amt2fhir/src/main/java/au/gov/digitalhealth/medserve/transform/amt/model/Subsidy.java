package au.gov.digitalhealth.medserve.transform.amt.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

public class Subsidy {

    private String pbsCode;
    private String programCode;
    private String commExManPrice;
    private String manExManPrice;
    private String restriction;
    private List<String> notes = new ArrayList<>();
    private List<String> caution = new ArrayList<>();
    private Set<Pair<String, String>> atcCodes = new HashSet<>();

    public Subsidy(String pbsCode, String programCode, String commExManPrice,
            String manExManPrice) {
        this.pbsCode = pbsCode;
        this.programCode = programCode;
        this.commExManPrice = commExManPrice;
        this.manExManPrice = manExManPrice;
    }

    public String getPbsCode() {
        return pbsCode;
    }

    public String getProgramCode() {
        return programCode;
    }

    public String getCommExManPrice() {
        return commExManPrice;
    }

    public String getManExManPrice() {
        return manExManPrice;
    }

    public String getRestriction() {
        return restriction;
    }

    public void setRestriction(String restriction) {
        this.restriction = restriction;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((commExManPrice == null) ? 0 : commExManPrice.hashCode());
        result = prime * result + ((manExManPrice == null) ? 0 : manExManPrice.hashCode());
        result = prime * result + ((pbsCode == null) ? 0 : pbsCode.hashCode());
        result = prime * result + ((programCode == null) ? 0 : programCode.hashCode());
        result = prime * result + ((restriction == null) ? 0 : restriction.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Subsidy other = (Subsidy) obj;
        if (commExManPrice == null) {
            if (other.commExManPrice != null)
                return false;
        } else if (!commExManPrice.equals(other.commExManPrice))
            return false;
        if (manExManPrice == null) {
            if (other.manExManPrice != null)
                return false;
        } else if (!manExManPrice.equals(other.manExManPrice))
            return false;
        if (pbsCode == null) {
            if (other.pbsCode != null)
                return false;
        } else if (!pbsCode.equals(other.pbsCode))
            return false;
        if (programCode == null) {
            if (other.programCode != null)
                return false;
        } else if (!programCode.equals(other.programCode))
            return false;
        if (restriction == null) {
            if (other.restriction != null)
                return false;
        } else if (!restriction.equals(other.restriction))
            return false;
        return true;
    }

    public void addNote(String string) {
        this.notes.add(string);
    }

    public void addCaution(String string) {
        this.caution.add(string);
    }

    public List<String> getNotes() {
        return notes;
    }

    public List<String> getCaution() {
        return caution;
    }

    public void addAtcCode(Pair<String, String> code) {
        this.atcCodes.add(code);
    }

    public Set<Pair<String, String>> getAtcCodes() {
        return this.atcCodes;
    }
}
