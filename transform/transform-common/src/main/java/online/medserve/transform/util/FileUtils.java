package online.medserve.transform.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class FileUtils {
    private static final Logger logger = Logger.getLogger(FileUtils.class.getCanonicalName());

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

    public static void readFile(Path path, Consumer<String[]> consumer, boolean hasHeader, String delimiter)
            throws IOException {
        Stream<String> stream = Files.lines(path);
        if (hasHeader) {
            stream = stream.skip(1);
        }
        stream.map(s -> s.split(delimiter, -1)).forEach(consumer);
        stream.close();
        logger.info("Processed " + path);
    }

    public static FileSystem getFileSystemForZipPath(Path path) throws IOException {
        return FileSystems.newFileSystem(
            URI.create("jar:file:" + path.toAbsolutePath().toString()),
            new HashMap<>());
    }

}
