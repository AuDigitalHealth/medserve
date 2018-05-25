package au.gov.digitalhealth.medserve.transform.amt.cache;

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

class TerminologyFileVisitor extends SimpleFileVisitor<Path> {

    private Path conceptFile, relationshipFile, descriptionFile, languageRefsetFile, artgIdRefsetFile,
            associationRefsetFile;
    private Set<Path> datatypePropertyFiles = new HashSet<Path>();

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
        if (attr.isRegularFile()) {
            String fileName = file.getFileName().toString();
            if (fileName.startsWith("sct2_Concept_Snapshot_AU1000036")) {
                conceptFile = file;
            } else if (fileName.startsWith("sct2_Relationship_Snapshot_AU1000036")) {
                relationshipFile = file;
            } else if (fileName.startsWith("sct2_Description_Snapshot-en-AU_AU1000036")) {
                descriptionFile = file;
            } else if (fileName.startsWith("der2_cRefset_LanguageSnapshot-en-AU_AU1000036")) {
                languageRefsetFile = file;
            } else if (fileName.startsWith("der2_iRefset_ARTGIdSnapshot_AU1000036")) {
                artgIdRefsetFile = file;
            } else if (fileName.startsWith("der2_ccsRefset_StrengthSnapshot_AU1000036")
                    || fileName.startsWith("der2_ccsRefset_UnitOfUseSizeSnapshot_AU1000036")
                    || fileName.startsWith("der2_ccsRefset_UnitOfUseQuantitySnapshot_AU1000036")
                    || fileName.startsWith("der2_cciRefset_SubpackQuantitySnapshot_AU1000036")) {
                datatypePropertyFiles.add(file);
            } else if (fileName.startsWith("der2_cRefset_AssociationReferenceSnapshot_AU1000036")) {
                associationRefsetFile = file;
            }
        }
        return FileVisitResult.CONTINUE;
    }

    public Path getConceptFile() {
        return conceptFile;
    }

    public Path getRelationshipFile() {
        return relationshipFile;
    }

    public Path getDescriptionFile() {
        return descriptionFile;
    }

    public Path getLanguageRefsetFile() {
        return languageRefsetFile;
    }

    public Set<Path> getDatatypePropertyFiles() {
        return datatypePropertyFiles;
    }

    public Path getArtgIdRefsetFile() {
        return artgIdRefsetFile;
    }

    public Path getAssociationRefsetFile() {
        return associationRefsetFile;
    }

}