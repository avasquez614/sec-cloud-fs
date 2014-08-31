package org.avasquez.seccloudfs.dropbox.db;

import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxWebAuthNoRedirect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.avasquez.seccloudfs.dropbox.db.model.DropboxCredential;
import org.avasquez.seccloudfs.dropbox.db.repos.DropboxCredentialRepository;
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
public class DropboxAuthorizerCli {

    private static final String CONTEXT_PATH = "application-context.xml";

    private BufferedReader stdIn;
    private PrintWriter stdOut;
    private DbxWebAuthNoRedirect webAuth;
    private DropboxCredentialRepository credentialRepository;

    public DropboxAuthorizerCli(BufferedReader stdIn, PrintWriter stdOut, DbxWebAuthNoRedirect webAuth,
                                DropboxCredentialRepository credentialRepository) {
        this.stdIn = stdIn;
        this.stdOut = stdOut;
        this.webAuth = webAuth;
        this.credentialRepository = credentialRepository;
    }

    public void run() {
        String authUrl = webAuth.start();
        String code = null;

        stdOut.println("1. Go to: " + authUrl);
        stdOut.println("2. Click \"Allow\" (you might have to log in first)");
        stdOut.print("3. Enter the authorization code: ");
        stdOut.flush();

        try {
            code = CliUtils.readLine(stdIn, stdOut);
        } catch (IOException e) {
            CliUtils.die("ERROR: Unable to read authorization code", e, stdOut);
        }

        try {
            DbxAuthFinish authFinish = webAuth.finish(code);
            DropboxCredential credential = new DropboxCredential(authFinish.accessToken);

            credentialRepository.insert(credential);

            stdOut.println("Credential successfully obtained and stored in DB with ID '" + credential.getId() + "'");
            stdOut.println();
        } catch (DbxException e) {
            CliUtils.die("ERROR: Unable to exchange authorization code for credential", e, stdOut);
        } catch (DbException e) {
            CliUtils.die("ERROR: Unable to store credential in DB", e, stdOut);
        }
    }

    public static void main(String... args) {
        ApplicationContext context = new ClassPathXmlApplicationContext(CONTEXT_PATH);
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter stdOut = new PrintWriter(System.out);
        DbxWebAuthNoRedirect auth = context.getBean(DbxWebAuthNoRedirect.class);
        DropboxCredentialRepository credentialRepository = context.getBean(DropboxCredentialRepository.class);
        DropboxAuthorizerCli cli = new DropboxAuthorizerCli(stdIn, stdOut, auth, credentialRepository);

        cli.run();
    }
    
}
