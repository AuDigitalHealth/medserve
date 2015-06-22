package amt2fhir.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileUtils {

	public static void initialiseOutputDirectories(Path baseDir, String type) throws IOException {
	    recursiveDelete(getSuccessPath(baseDir, type));
	    Files.createDirectories(getSuccessPath(baseDir, type));
	    recursiveDelete(getFailPath(baseDir, type));
	    Files.createDirectories(getFailPath(baseDir, type));
	}

	public static void recursiveDelete(Path path) throws IOException {
	    if (Files.exists(path)) {
	        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
	            @Override
	            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
	                Files.delete(file);
	                return FileVisitResult.CONTINUE;
	            }
	
	            @Override
	            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
	                Files.delete(dir);
	                return FileVisitResult.CONTINUE;
	            }
	
	        });
	    }
	}

	public static Path getSuccessPath(Path basePath, String type) {
	    return basePath.getFileSystem().getPath(basePath.toString(), type);
	}

	public static Path getFailPath(Path basePath, String type) {
	    return basePath.getFileSystem().getPath(basePath.toString(), type, "fail");
	}

}
