package org.avasquez.seccloudfs.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alfonsovasquez on 02/02/14.
 */
public class FileUtils {

    public static final Pattern humanReadableSizePattern = Pattern.compile("(\\d+)\\s*(B|KB|MB|GB)?",
            Pattern.CASE_INSENSITIVE);

    private FileUtils() {
    }

    public static long sizeOfDirectory(Path directory) throws IOException {
        SizeCalculatingFileVisitor visitor = new SizeCalculatingFileVisitor();
        Files.walkFileTree(directory, visitor);

        return visitor.totalSize;
    }

    public static long humanReadableByteSizeToByteCount(String size) {
        Matcher sizeMatcher = humanReadableSizePattern.matcher(size);
        if (sizeMatcher.matches()) {
            try {
                long coeff = Long.parseLong(sizeMatcher.group(1));
                int unit = 1024;
                int exp;
                String suffix = sizeMatcher.group(2).toUpperCase();

                switch (suffix) {
                    case "KB":
                        exp = 1;
                        break;
                    case "MB":
                        exp = 2;
                        break;
                    case "GB":
                        exp = 3;
                        break;
                    default:
                        exp = 0;
                        break;
                }


                return (long) (coeff * Math.pow(unit, exp));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid human-readable size '" + size + "'");
            }
        } else {
            throw new IllegalArgumentException("Invalid human-readable size '" + size + "'");
        }
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
