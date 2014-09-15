package org.avasquez.seccloudfs.gdrive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredentials;
import org.avasquez.seccloudfs.gdrive.db.repos.GoogleDriveCredentialsRepository;
import org.avasquez.seccloudfs.utils.CliUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Simple command-line app used to authorize the Sec Cloud FS to access Google Drive accounts through OAuth2.
 * Authorization information is then stored in the DB for later use by the system.
 *
 * @author avasquez
 */
public class GoogleDriveAuthorizerApp {

    private static final String CONTEXT_PATH = "application-context.xml";

    private BufferedReader stdIn;
    private PrintWriter stdOut;
    private GoogleDriveAuthorizationSupport authSupport;
    private GoogleDriveCredentialsRepository credentialsRepository;

    public GoogleDriveAuthorizerApp(BufferedReader stdIn, PrintWriter stdOut, GoogleDriveAuthorizationSupport
        authSupport, GoogleDriveCredentialsRepository credentialsRepository) {
        this.stdIn = stdIn;
        this.stdOut = stdOut;
        this.authSupport = authSupport;
        this.credentialsRepository = credentialsRepository;
    }

    public void run() {
        String authUrl = authSupport.getAuthorizationUrl();

        stdOut.println("1. Go to: " + authUrl);
        stdOut.println("2. Click \"Allow\" (you might have to log in first)");
        stdOut.print("3. Enter authorization code: ");
        stdOut.flush();

        String code = null;
        try {
            code = CliUtils.readLine(stdIn, stdOut);
        } catch (IOException e) {
            CliUtils.die("ERROR: Unable to read authorization code input", e, stdOut);
        }

        GoogleDriveCredentials credentials = null;
        try {
            credentials = authSupport.exchangeCode(code);
        } catch (IOException e) {
            CliUtils.die("ERROR: Unable to exchange authorization code for credentials", e, stdOut);
        }

        stdOut.print("4. Enter the account ID: ");
        stdOut.flush();

        String accountId = null;
        try {
            accountId = CliUtils.readLine(stdIn, stdOut);
        } catch (IOException e) {
            CliUtils.die("ERROR: Unable to read account ID input", e, stdOut);
        }

        credentials.setAccountId(accountId);

        try {
            credentialsRepository.insert(credentials);

            stdOut.println("Credentials successfully obtained and stored in DB with ID '" + credentials.getId() + "'");
            stdOut.flush();
        } catch (DbException e) {
            CliUtils.die("ERROR: Unable to store credentials in DB", e, stdOut);
        }
    }

    public static void main(String... args) {
        ApplicationContext context = new ClassPathXmlApplicationContext(CONTEXT_PATH);
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter stdOut = new PrintWriter(System.out);
        GoogleDriveAuthorizationSupport authSupport = context.getBean(GoogleDriveAuthorizationSupport.class);
        GoogleDriveCredentialsRepository credentialsRepository = context.getBean(GoogleDriveCredentialsRepository.class);
        GoogleDriveAuthorizerApp app = new GoogleDriveAuthorizerApp(stdIn, stdOut, authSupport, credentialsRepository);

        app.run();
    }

}
