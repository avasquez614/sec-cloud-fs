package org.avasquez.seccloudfs.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alfonsovasquez on 02/02/14.
 */
public class FileUtils {

    public static final long ONE_KB = 1024;
    public static final long ONE_MB = 1024 * ONE_KB;
    public static final long ONE_GB = 1024 * ONE_MB;
    public static final long ONE_TB = 1024 * ONE_GB;
    public static final long ONE_PB = 1024 * ONE_TB;
    public static final long ONE_EB = 1024 * ONE_PB;

    public static final Pattern HUMAN_READABLE_SIZE_PATTERN =
        Pattern.compile("(\\d+)\\s*(B|KB|MB|GB|TB|PB|EB)?", Pattern.CASE_INSENSITIVE);

    public static final OpenOption[] TMP_FILE_OPEN_OPTIONS = {
        StandardOpenOption.READ,
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.DELETE_ON_CLOSE
    };

    private FileUtils() {
    }

    public static long sizeOfDirectory(Path directory) throws IOException {
        SizeCalculatingFileVisitor visitor = new SizeCalculatingFileVisitor();
        Files.walkFileTree(directory, visitor);

        return visitor.totalSize;
    }

    public static String byteCountToHumanReadableByteSize(long size) {
        if (size / ONE_EB > 0) {
            return "" + BigDecimal.valueOf((double)size / (double)ONE_EB).setScale(2, RoundingMode.CEILING) + " EB";
        } else if (size / ONE_PB > 0) {
            return "" + BigDecimal.valueOf((double)size / (double)ONE_PB).setScale(2, RoundingMode.CEILING) + " PB";
        } else if (size / ONE_TB > 0) {
            return "" + BigDecimal.valueOf((double)size / (double)ONE_TB).setScale(2, RoundingMode.CEILING) + " TB";
        } else if (size / ONE_GB > 0) {
            return "" + BigDecimal.valueOf((double)size / (double)ONE_GB).setScale(2, RoundingMode.CEILING) + " GB";
        } else if (size / ONE_MB > 0) {
            return "" + BigDecimal.valueOf((double)size / (double)ONE_MB).setScale(2, RoundingMode.CEILING) + " MB";
        } else if (size / ONE_KB > 0) {
            return "" + BigDecimal.valueOf((double)size / (double)ONE_KB).setScale(2, RoundingMode.CEILING) + " KB";
        } else {
            return String.valueOf(size) + " B";
        }
    }

    public static long humanReadableByteSizeToByteCount(String size) {
        Matcher sizeMatcher = HUMAN_READABLE_SIZE_PATTERN.matcher(size);
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
                    case "TB":
                        exp = 4;
                        break;
                    case "PB":
                        exp = 5;
                        break;
                    case "EB":
                        exp = 6;
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
