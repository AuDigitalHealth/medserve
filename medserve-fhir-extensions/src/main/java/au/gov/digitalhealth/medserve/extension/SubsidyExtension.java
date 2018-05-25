package au.gov.digitalhealth.medserve.extension;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.BackboneElement;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DecimalType;

import ca.uhn.fhir.model.api.annotation.Block;
import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.api.annotation.Extension;
import ca.uhn.fhir.util.ElementUtil;

@Block
public class SubsidyExtension extends BackboneElement {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * A coding for the subsidy from the subsidy provider
     */
    @Child(name = "subsidyCode", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/subsidyCode", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "A coding for the subsidy from the subsidy provider")
    private Coding subsidyCode;

    /**
     * A coding for the program the subsidy is for
     */
    @Child(name = "programCode", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/programCode", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "A coding for the program the subsidy is for")
    private Coding programCode;

    /**
     * A coding for the types of prescribers who can prescribe this medication
     */
    @Child(name = "prescriberTypes", min = 0, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/prescriberTypes", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "A coding for the types of prescribers who can prescribe this medication")
    private List<Coding> prescriberTypes;

    /**
     * Notes that go with the subsidy
     */
    @Child(name = "notes", min = 0, max = Child.MAX_UNLIMITED, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/subsidyNotes", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "Notes that go with the subsidy")
    private List<Annotation> notes;

    /**
     * Cautionary notes that go with the subsidy
     */
    @Child(name = "cautionaryNotes", min = 0, max = Child.MAX_UNLIMITED, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/cautionaryNotes", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "Cautionary notes that go with the subsidy")
    private List<Annotation> cautionaryNotes;

    /**
     * Restriction that apply to the subsidy
     */
    @Child(name = "restriction", min = 0, max = Child.MAX_UNLIMITED, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/restriction", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "Restriction that apply to the subsidy")
    private Coding restriction;

    /**
     * Commonwealth ex-manufacturer price
     */
    @Child(name = "commonwealthExManufacturerPrice", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/commonwealthExManufacturerPrice", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "Commonwealth ex-manufacturer price")
    private DecimalType commonwealthExManufacturerPrice;

    /**
     * Manufacturer ex-manufacturer price
     */
    @Child(name = "manufacturerExManufacturerPrice", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/manufacturerExManufacturerPrice", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "Manufacturer ex-manufacturer price")
    private DecimalType manufacturerExManufacturerPrice;

    /**
     * ATC code/s
     */
    @Child(name = "atcCode", min = 1, max = 1, summary = false)
    @Extension(url = ExtendedMedication.PROFILE_URL_BASE
            + "StructureDefinition/atcCode", definedLocally = false, isModifier = false)
    @Description(shortDefinition = "ATC code")
    private CodeableConcept atcCode;

    /**
     * It is important to override the isEmpty() method, adding a check for any newly added fields.
     */
    @Override
    public boolean isEmpty() {
        return super.isEmpty() && ElementUtil.isEmpty(subsidyCode, programCode, prescriberTypes, notes, cautionaryNotes,
            restriction, commonwealthExManufacturerPrice, manufacturerExManufacturerPrice);
    }

    @Override
    public SubsidyExtension copy() {
        SubsidyExtension copy = new SubsidyExtension();
        copy.subsidyCode = subsidyCode;
        copy.prescriberTypes = prescriberTypes;
        copy.programCode = programCode;
        copy.notes = notes;
        copy.restriction = restriction;
        copy.cautionaryNotes = cautionaryNotes;
        copy.commonwealthExManufacturerPrice = commonwealthExManufacturerPrice;
        copy.manufacturerExManufacturerPrice = manufacturerExManufacturerPrice;
        copy.atcCode = atcCode;
        return copy;
    }

    public Coding getSubsidyCode() {
        return subsidyCode;
    }

    public void setSubsidyCode(Coding subsidyCode) {
        this.subsidyCode = subsidyCode;
    }

    public List<Coding> getPrescriberTypes() {
        return prescriberTypes;
    }

    public void setPrescriberTypes(List<Coding> prescriberTypes) {
        this.prescriberTypes = prescriberTypes;
    }

    public List<Annotation> getNotes() {
        return notes;
    }

    public void setNotes(List<Annotation> notes) {
        this.notes = notes;
    }

    public List<Annotation> getCautionaryNotes() {
        return cautionaryNotes;
    }

    public void setCautionaryNotes(List<Annotation> cautionaryNotes) {
        this.cautionaryNotes = cautionaryNotes;
    }

    public Coding getRestriction() {
        return restriction;
    }

    public void setRestriction(Coding restriction) {
        this.restriction = restriction;
    }

    public Coding getProgramCode() {
        return programCode;
    }

    public void setProgramCode(Coding programCode) {
        this.programCode = programCode;
    }

    public DecimalType getCommonwealthExManufacturerPrice() {
        return commonwealthExManufacturerPrice;
    }

    public void setCommonwealthExManufacturerPrice(DecimalType commonwealthExManufacturerPrice) {
        this.commonwealthExManufacturerPrice = commonwealthExManufacturerPrice;
    }

    public DecimalType getManufacturerExManufacturerPrice() {
        return manufacturerExManufacturerPrice;
    }

    public void setManufacturerExManufacturerPrice(DecimalType manufacturerExManufacturerPrice) {
        this.manufacturerExManufacturerPrice = manufacturerExManufacturerPrice;
    }

    public void addNote(Annotation note) {
        if (notes == null) {
            notes = new ArrayList<>();
        }
        notes.add(note);
    }

    public void addCautionaryNote(Annotation note) {
        if (cautionaryNotes == null) {
            cautionaryNotes = new ArrayList<>();
        }
        cautionaryNotes.add(note);
    }

    public CodeableConcept getAtcCode() {
        return atcCode;
    }

    public void setAtcCode(CodeableConcept atcCode) {
        this.atcCode = atcCode;
    }
}
