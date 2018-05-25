package au.gov.digitalhealth.medserve.transform.rxnorm.cache;

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class RxNormFileVisitor extends SimpleFileVisitor<Path> {
    private Path conceptFile, relationshipFile, attributeFile;

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
        if (attr.isRegularFile()) {
            String fileName = file.getFileName().toString();
            if (fileName.equals("RXNCONSO.RRF")) {
                conceptFile = file;
            } else if (fileName.equals("RXNREL.RRF")) {
                relationshipFile = file;
            } else if (fileName.equals("RXNSAT.RRF")) {
                attributeFile = file;
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

    public Path getAttributeFile() {
        return attributeFile;
    }

}
