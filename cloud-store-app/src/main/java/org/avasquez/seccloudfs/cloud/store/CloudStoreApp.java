package org.avasquez.seccloudfs.cloud.store;

import org.apache.commons.cli.*;
import org.avasquez.seccloudfs.cloud.CloudStore;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Command line application for uploading, downloading and deleting files directly through a {@link org.avasquez
 * .seccloudfs.cloud.CloudStore}.
 *
 * @author avasquez
 */
public class CloudStoreApp {

    private static final String CONTEXT_PATH = "application-context.xml";

    private CloudStore cloudStore;
    private Options options;
    private CommandLineParser commandLineParser;

    @SuppressWarnings("AccessStaticViaInstance")
    public CloudStoreApp(CloudStore cloudStore) {
        this.cloudStore = cloudStore;

        Option upload = OptionBuilder
            .withArgName("id> <file")
            .hasArgs(2)
            .withDescription("Upload the given <file> with the specified <id> to the cloud store")
            .create("upload");

        Option download = OptionBuilder
            .withArgName("id> <file")
            .hasArgs(2)
            .withDescription("Download the file with the specified <id> from the cloud store to <file> path")
            .create("download");

        Option delete = OptionBuilder
            .withArgName("id")
            .hasArgs(1)
            .withDescription("Delete the file with the specified <id> from the cloud store")
            .create("delete");

        options = new Options();
        options.addOption("help", false, "Prints this message");
        options.addOption(upload);
        options.addOption(download);
        options.addOption(delete);

        commandLineParser = new BasicParser();
    }

    public void run(String... args) {
        try {
            CommandLine commandLine = commandLineParser.parse(options, args);
            if (commandLine.hasOption("upload")) {
                String[] values = commandLine.getOptionValues("upload");
                String id = values[0];
                String file = values[1];

                uploadFile(id, file, cloudStore);
            } else if (commandLine.hasOption("download")) {
                String[] values = commandLine.getOptionValues("download");
                String id = values[0];
                String file = values[1];

                downloadFile(id, file, cloudStore);
            } else if (commandLine.hasOption("delete")) {
                String id = commandLine.getOptionValue("delete");

                deleteFile(id, cloudStore);
            } else if (commandLine.hasOption("help")) {
                printHelp();
            } else {
                dieWithHelpInfo("ERROR: Unrecognized command line option");
            }
        } catch (ParseException e) {
            die("ERROR: Command line parsing failed", e);
        }
    }

    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("./cloud-store-app", options);
    }

    private void uploadFile(String id, String file, CloudStore cloudStore) {
        Path path = Paths.get(file);

        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)) {
            cloudStore.upload(id, fileChannel, fileChannel.size());
        } catch (IOException e) {
            die("ERROR: Unable to upload file", e);
        }
    }

    private void downloadFile(String id, String file, CloudStore cloudStore) {
        Path path = Paths.get(file);

        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
             StandardOpenOption.TRUNCATE_EXISTING)) {
            cloudStore.download(id, fileChannel);
        } catch (IOException e) {
            die("ERROR: Unable to download file", e);
        }
    }

    private void deleteFile(String id, CloudStore cloudStore) {
        try {
            cloudStore.delete(id);
        } catch (IOException e) {
            die("ERROR: Unable to delete file", e);
        }
    }

    private void die(String message, Throwable e) {
        System.out.println(message);

        e.printStackTrace();

        System.exit(1);
    }

    private void dieWithHelpInfo(String message) {
        System.out.println(message);

        printHelp();

        System.exit(1);
    }

    public static void main(String... args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(CONTEXT_PATH);
        context.registerShutdownHook();

        CloudStore cloudStore = context.getBean(CloudStore.class);
        CloudStoreApp app = new CloudStoreApp(cloudStore);

        app.run(args);
    }

}
