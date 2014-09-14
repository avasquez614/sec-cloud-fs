package org.avasquez.seccloudfs.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxWebAuthNoRedirect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.avasquez.seccloudfs.dropbox.db.model.DropboxCredentials;
import org.avasquez.seccloudfs.dropbox.db.repos.DropboxCredentialsRepository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.utils.CliUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Simple command-line app used to authorize the Sec Cloud FS to access Dropbox accounts through OAuth2.
 * Authorization information is then stored in the DB for later use by the system.
 *
 * @author avasquez
 */
public class DropboxAuthorizerApp {

    private static final String CONTEXT_PATH = "application-context.xml";

    private BufferedReader stdIn;
    private PrintWriter stdOut;
    private DbxWebAuthNoRedirect webAuth;
    private DropboxCredentialsRepository credentialsRepository;

    public DropboxAuthorizerApp(BufferedReader stdIn, PrintWriter stdOut, DbxWebAuthNoRedirect webAuth,
                                DropboxCredentialsRepository credentialsRepository) {
        this.stdIn = stdIn;
        this.stdOut = stdOut;
        this.webAuth = webAuth;
        this.credentialsRepository = credentialsRepository;
    }

    public void run() {
        String authUrl = webAuth.start();

        stdOut.println("1. Go to: " + authUrl);
        stdOut.println("2. Click \"Allow\" (you might have to log in first)");
        stdOut.print("3. Enter the authorization code: ");
        stdOut.flush();

        String code = null;
        try {
            code = CliUtils.readLine(stdIn, stdOut);
        } catch (IOException e) {
            CliUtils.die("ERROR: Unable to read authorization code input", e, stdOut);
        }

        String accessToken = null;
        try {
            accessToken = webAuth.finish(code).accessToken;
        } catch (DbxException e) {
            CliUtils.die("ERROR: Unable to exchange authorization code for credentials", e, stdOut);
        }

        DropboxCredentials credentials = new DropboxCredentials();
        credentials.setAccessToken(accessToken);

        stdOut.print("4. Enter the username: ");
        stdOut.flush();

        String username = null;
        try {
            username = CliUtils.readLine(stdIn, stdOut);
        } catch (IOException e) {
            CliUtils.die("ERROR: Unable to read username input", e, stdOut);
        }

        credentials.setUsername(username);

        try {
            credentialsRepository.insert(credentials);

            stdOut.println("Credentials successfully obtained and stored in DB with ID '" + credentials.getId() + "'");
            stdOut.println();
        } catch (DbException e) {
            CliUtils.die("ERROR: Unable to store credentials in DB", e, stdOut);
        }
    }

    public static void main(String... args) {
        ApplicationContext context = new ClassPathXmlApplicationContext(CONTEXT_PATH);
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter stdOut = new PrintWriter(System.out);
        DbxWebAuthNoRedirect auth = context.getBean(DbxWebAuthNoRedirect.class);
        DropboxCredentialsRepository credentialRepository = context.getBean(DropboxCredentialsRepository.class);
        DropboxAuthorizerApp app = new DropboxAuthorizerApp(stdIn, stdOut, auth, credentialRepository);

        app.run();
    }
    
}
