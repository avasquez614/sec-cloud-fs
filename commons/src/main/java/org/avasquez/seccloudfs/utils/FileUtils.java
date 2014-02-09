package org.avasquez.seccloudfs.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by alfonsovasquez on 02/02/14.
 */
public class FileUtils {

    private FileUtils() {
    }

    public static long sizeOfDirectory(Path directory) throws IOException {
        SizeCalculatingFileVisitor visitor = new SizeCalculatingFileVisitor();
        Files.walkFileTree(directory, visitor);

        return visitor.totalSize;
    }

    private static class SizeCalculatingFileVisitor extends SimpleFileVisitor<Path> {

        private long totalSize;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            totalSize += attrs.size();

            return FileVisitResult.CONTINUE;
        }

    }

}
