package org.avasquez.seccloudfs.cloud.store;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.avasquez.seccloudfs.cloud.CloudStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Command line application for uploading, downloading and deleting files directly through a {@link org.avasquez
 * .seccloudfs.cloud.CloudStore}.
 *
 * @author avasquez
 */
public class CloudStoreApp {

    private static final Logger logger = LoggerFactory.getLogger(CloudStoreApp.class);

    private static final String CONTEXT_PATH = "application-context.xml";

    private CloudStore cloudStore;
    private Options options;
    private CommandLineParser commandLineParser;

    @SuppressWarnings("AccessStaticViaInstance")
    public CloudStoreApp(CloudStore cloudStore) {
        this.cloudStore = cloudStore;

        Option upload = OptionBuilder
            .withDescription("Upload the files to the cloud store")
            .create("upload");

        Option download = OptionBuilder
            .withDescription("Download the files from the cloud store")
            .create("download");

        Option delete = OptionBuilder
            .withDescription("Delete the files from the cloud store")
            .create("delete");

        Option basePath = OptionBuilder
            .withArgName("basepath")
            .hasArg(true)
            .withDescription("The base path for the files in the list (required for upload and download)")
            .create("basepath");

        Option fileList = OptionBuilder
            .withArgName("filelist")
            .hasArg(true)
            .withDescription("The list of the files to upload, download or delete")
            .create("filelist");

        options = new Options();
        options.addOption("help", false, "Prints this message");
        options.addOption(upload);
        options.addOption(download);
        options.addOption(delete);
        options.addOption(basePath);
        options.addOption(fileList);

        commandLineParser = new BasicParser();
    }

    public void run(String... args) {
        try {
            CommandLine commandLine = commandLineParser.parse(options, args);
            if (commandLine.hasOption("upload")) {
                String basePath = getBasePath(commandLine);
                List<String> files = getFileList(commandLine);

                uploadFiles(basePath, files, cloudStore);
            } else if (commandLine.hasOption("download")) {
                String basePath = getBasePath(commandLine);
                List<String> files = getFileList(commandLine);

                downloadFiles(basePath, files, cloudStore);
            } else if (commandLine.hasOption("delete")) {
                List<String> files = getFileList(commandLine);

                deleteFiles(files, cloudStore);
            } else if (commandLine.hasOption("help")) {
                printHelp();
            } else {
                dieWithHelpInfo("ERROR: Unrecognized command line option");
            }
        } catch (ParseException e) {
            die("ERROR: Command line parsing failed", e);
        }
    }

    private String getBasePath(CommandLine commandLine) {
        if (commandLine.hasOption("basepath")) {
            return commandLine.getOptionValue("basepath");
        } else {
            dieWithHelpInfo("ERROR: basepath command line option is required");
        }

        // Will never happen
        return null;
    }

    private List<String> getFileList(CommandLine commandLine) {
        if (commandLine.hasOption("filelist")) {
            String fileListFile = commandLine.getOptionValue("filelist");

            try {
                return FileUtils.readLines(new File(fileListFile), "UTF-8");
            } catch (IOException e) {
                die("ERROR: Unable to read file list file " + fileListFile, e);
            }
        } else {
            dieWithHelpInfo("ERROR: filelist command line option is required");
        }

        // Will never happen
        return null;
    }

    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("./cloud-store-app", options);
    }

    private void uploadFiles(String basePath, List<String> files, CloudStore cloudStore) {
        logger.info("Started bulk upload");

        for (String file : files) {
            uploadFile(basePath, file, cloudStore);
        }

        logger.info("Finished bulk upload");
    }

    private void uploadFile(String basePath, String id, CloudStore cloudStore) {
        Path path = Paths.get(basePath, id);

        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)) {
            cloudStore.upload(id, fileChannel, fileChannel.size());
        } catch (IOException e) {
            die("ERROR: Unable to upload file", e);
        }
    }

    private void downloadFiles(String basePath, List<String> files, CloudStore cloudStore) {
        logger.info("Started bulk download");

        for (String file : files) {
            downloadFile(basePath, file, cloudStore);
        }

        logger.info("Finished bulk download");
    }

    private void downloadFile(String basePath, String id, CloudStore cloudStore) {
        Path path = Paths.get(basePath, id);

        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
             StandardOpenOption.TRUNCATE_EXISTING)) {
            cloudStore.download(id, fileChannel);
        } catch (IOException e) {
            die("ERROR: Unable to download file", e);
        }
    }

    private void deleteFiles(List<String> files, CloudStore cloudStore) {
        logger.info("Started bulk delete");

        for (String file : files) {
            deleteFile(file, cloudStore);
        }

        logger.info("Finished bulk delete");
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
