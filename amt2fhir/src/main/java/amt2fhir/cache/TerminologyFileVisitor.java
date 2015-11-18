package amt2fhir.cache;

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

class TerminologyFileVisitor extends SimpleFileVisitor<Path> {

    private Path conceptFile, relationshipFile, descriptionFile, languageRefsetFile, artgIdRefsetFile;
    private Set<Path> datatypePropertyFiles = new HashSet<Path>();

    // Print information about
    // each type of file.
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
        if (attr.isRegularFile()) {
            String fileName = file.getFileName().toString();
            if (fileName.startsWith("sct2_Concept_Snapshot_AU1000168")) {
                conceptFile = file;
            } else if (fileName.startsWith("sct2_Relationship_Snapshot_AU1000168")) {
                relationshipFile = file;
            } else if (fileName.startsWith("sct2_Description_Snapshot-en-AU_AU1000168")) {
                descriptionFile = file;
            } else if (fileName.startsWith("der2_cRefset_LanguageSnapshot-en-AU_AU1000168")) {
                languageRefsetFile = file;
            } else if (fileName.startsWith("der2_iRefset_ARTGIdSnapshot_AU1000168")) {
                artgIdRefsetFile = file;
            } else if (fileName.startsWith("der2_ccsRefset_StrengthSnapshot_AU1000168")
                || fileName.startsWith("der2_ccsRefset_UnitOfUseSizeSnapshot_AU1000168")
                || fileName.startsWith("der2_ccsRefset_UnitOfUseQuantitySnapshot_AU1000168")
                || fileName.startsWith("der2_cciRefset_SubpackQuantitySnapshot_AU1000168")) {
                datatypePropertyFiles.add(file);
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

}