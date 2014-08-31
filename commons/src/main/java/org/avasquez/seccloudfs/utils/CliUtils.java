package org.avasquez.seccloudfs.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.lang.StringUtils;

/**
 * Utility methods for CLI applications.
 *
 * @author avasquez
 */
public class CliUtils {

    private CliUtils() {
    }

    /**
     * Reads a line from the standard input. Will print an error until the user has specified an non-blank line.
     *
     * @param stdIn     the standard input reader
     * @param stdOut    the standard output writer
     *
     * @return the read line
     */
    public static String readLine(BufferedReader stdIn, PrintWriter stdOut) throws IOException {
        String in = "";

        while (StringUtils.isBlank(in)) {
            in = stdIn.readLine();
            if (StringUtils.isBlank(in)) {
                stdOut.println("ERROR: No input received");
                stdOut.flush();
            }
        }

        return in.trim();
    }

    /**
     * Kills the program, printing first the specified message and exception stack trace on standard output.
     *
     * @param message   the message to print
     * @param e         the exception
     * @param stdOut    the standard output writer
     */
    public static void die(String message, Throwable e, PrintWriter stdOut) {
        stdOut.println(message);
        stdOut.flush();

        e.printStackTrace(stdOut);

        System.exit(1);
    }

}
