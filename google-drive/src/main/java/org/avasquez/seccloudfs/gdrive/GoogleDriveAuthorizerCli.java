package org.avasquez.seccloudfs.gdrive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.apache.commons.lang.StringUtils;
import org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredential;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Simple command-line app used to authorize the Sec Cloud FS to access Google Drive accounts through OAuth2.
 * Authorization information is then stored in the DB for later use by the system.
 *
 * @author avasquez
 */
public class GoogleDriveAuthorizerCli {

    private static final String CONTEXT_PATH = "application-context.xml";

    private BufferedReader stdIn;
    private PrintWriter stdOut;
    private GoogleDriveAuthorizationSupport authorizationSupport;

    public GoogleDriveAuthorizerCli(final BufferedReader stdIn, final PrintWriter stdOut,
                                    final GoogleDriveAuthorizationSupport authorizationSupport) {
        this.stdIn = stdIn;
        this.stdOut = stdOut;
        this.authorizationSupport = authorizationSupport;
    }

    public void run() {
        String code = null;
        try {
            String authUrl = authorizationSupport.getAuthorizationUrl();

            stdOut.print("1. Please open the following URL in your browser: ");
            stdOut.println(authUrl);
            stdOut.print("2. Type authorization code: ");
            stdOut.flush();

            code = readLine();
        } catch (IOException e) {
            die("ERROR: Unable to read authorization code", e);
        }

        try {
            GoogleDriveCredential credential = authorizationSupport.exchangeCode(code);

            stdOut.println("Credential successfully obtained and stored in DB with ID '" + credential.getId() + "'");
            stdOut.println();
        } catch (IOException e) {
            die("ERROR: Unable to exchange authorization code for credential", e);
        }
    }

    private String readLine() throws IOException {
        String in = "";

        while (StringUtils.isBlank(in)) {
            in = stdIn.readLine();
            if (StringUtils.isBlank(in)) {
                stdOut.println("ERROR: No input received");
                stdOut.flush();
            }
        }

        return in;
    }

    private void die(String message, Throwable e) {
        stdOut.println(message);
        stdOut.flush();

        e.printStackTrace(stdOut);

        System.exit(1);
    }

    public static void main(String... args) {
        ApplicationContext context = getApplicationContext();
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter stdOut = new PrintWriter(System.out);
        GoogleDriveAuthorizationSupport authorizationSupport = context.getBean(GoogleDriveAuthorizationSupport.class);
        GoogleDriveAuthorizerCli cli = new GoogleDriveAuthorizerCli(stdIn, stdOut, authorizationSupport);

        cli.run();
    }

    public static ApplicationContext getApplicationContext() {
        ApplicationContext context = new ClassPathXmlApplicationContext(CONTEXT_PATH);

        return context;
    }

}
