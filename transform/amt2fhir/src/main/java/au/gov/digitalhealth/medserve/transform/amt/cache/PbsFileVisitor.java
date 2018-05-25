package au.gov.digitalhealth.medserve.transform.amt.cache;

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

class PbsFileVisitor extends SimpleFileVisitor<Path> {

    private Path amtFile, atcFile, cautionFile, noteFile, continuedDispensingFile, dispensingInsentiveFile, drugFile,
            indicationFile, manufacturerFile, pbsItemFile, restrictionFile;

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
        if (attr.isRegularFile()) {
            String fileName = file.getFileName().toString();
            if (fileName.matches("amt_.*.txt")) {
                amtFile = file;
            } else if (fileName.startsWith("atc_")) {
                atcFile = file;
            } else if (fileName.startsWith("CautionExtract_")) {
                cautionFile = file;
            } else if (fileName.startsWith("NoteExtract_")) {
                noteFile = file;
            } else if (fileName.startsWith("cd_")) {
                continuedDispensingFile = file;
            } else if (fileName.startsWith("DI_")) {
                dispensingInsentiveFile = file;
            } else if (fileName.startsWith("drug_")) {
                drugFile = file;
            } else if (fileName.startsWith("LinkExtract_")) {
                indicationFile = file;
            } else if (fileName.startsWith("mnfr_")) {
                manufacturerFile = file;
            } else if (fileName.startsWith("PBS_Item_Table_")) {
                pbsItemFile = file;
            } else if (fileName.startsWith("RestrictionExtract_")) {
                restrictionFile = file;
            }
        }
        return FileVisitResult.CONTINUE;
    }

    public Path getAmtFile() {
        return amtFile;
    }

    public Path getAtcFile() {
        return atcFile;
    }

    public Path getCautionFile() {
        return cautionFile;
    }

    public Path getNoteFile() {
        return noteFile;
    }

    public Path getContinuedDispensingFile() {
        return continuedDispensingFile;
    }

    public Path getDispensingInsentiveFile() {
        return dispensingInsentiveFile;
    }

    public Path getDrugFile() {
        return drugFile;
    }

    public Path getIndicationFile() {
        return indicationFile;
    }

    public Path getManufacturerFile() {
        return manufacturerFile;
    }

    public Path getPbsItemFile() {
        return pbsItemFile;
    }

    public Path getRestrictionFile() {
        return restrictionFile;
    }

}