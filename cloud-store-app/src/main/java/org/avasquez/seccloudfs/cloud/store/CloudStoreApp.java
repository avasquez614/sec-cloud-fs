package org.avasquez.seccloudfs.cloud.store;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
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

        Option path = OptionBuilder
            .withArgName("path")
            .hasArg(true)
            .withDescription("The path for the <file> or files in <filelist> (required for upload and download)")
            .create("path");

        Option id = OptionBuilder
            .withArgName("id")
            .hasArg(true)
            .withDescription("The ID of the single file to upload, download or delete")
            .create("id");

        Option fileList = OptionBuilder
            .withArgName("filelist")
            .hasArg(true)
            .withDescription("The list of IDs of the files to upload, download or delete")
            .create("filelist");

        options = new Options();
        options.addOption("help", false, "Prints this message");
        options.addOption(upload);
        options.addOption(download);
        options.addOption(delete);
        options.addOption(path);
        options.addOption(id);
        options.addOption(fileList);

        commandLineParser = new BasicParser();
    }

    public void run(String... args) {
        try {
            CommandLine commandLine = commandLineParser.parse(options, args);
            if (commandLine.hasOption("upload")) {
                if (commandLine.hasOption("id")) {
                    uploadFile(getPath(commandLine), getId(commandLine), cloudStore);
                } else {
                    uploadFiles(getPath(commandLine), getFileList(commandLine), cloudStore);
                }
            } else if (commandLine.hasOption("download")) {
                if (commandLine.hasOption("id")) {
                    downloadFile(getPath(commandLine), getId(commandLine), cloudStore);
                } else {
                    downloadFiles(getPath(commandLine), getFileList(commandLine), cloudStore);
                }
            } else if (commandLine.hasOption("delete")) {
                if (commandLine.hasOption("id")) {
                    deleteFile(getId(commandLine), cloudStore);
                } else {
                    deleteFiles(getFileList(commandLine), cloudStore);
                }
            } else if (commandLine.hasOption("help")) {
                printHelp();
            } else {
                dieWithHelpInfo("ERROR: Unrecognized command line option");
            }
        } catch (ParseException e) {
            die("ERROR: Command line parsing failed", e);
        }
    }

    private String getPath(CommandLine commandLine) {
        String path = commandLine.getOptionValue("path");
        if (StringUtils.isNotEmpty(path)) {
            path = StringUtils.stripEnd(path, SystemUtils.LINE_SEPARATOR);
        } else {
            dieWithHelpInfo("ERROR: path command line option is required");
        }

        return path;
    }

    private String getId(CommandLine commandLine) {
        String id = commandLine.getOptionValue("id");
        if (StringUtils.isEmpty(id)) {
            dieWithHelpInfo("ERROR: id command line option is required");
        }

        return id;
    }

    private List<String> getFileList(CommandLine commandLine) {
        List<String> fileList = null;
        String fileListFile = commandLine.getOptionValue("filelist");

        if (StringUtils.isNotEmpty(fileListFile)) {
            try {
                fileList = FileUtils.readLines(new File(fileListFile), "UTF-8");
            } catch (IOException e) {
                die("ERROR: Unable to read file list file " + fileListFile, e);
            }
        } else {
            dieWithHelpInfo("ERROR: filelist command line option is required");
        }

        return fileList;
    }

    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("./cloud-store-app", options);
    }

    private void uploadFiles(String path, List<String> files, CloudStore cloudStore) {
        logger.info("Started bulk upload");

        for (String file : files) {
            uploadFile(path + SystemUtils.FILE_SEPARATOR + file, file, cloudStore);
        }

        logger.info("Finished bulk upload");
    }

    private void uploadFile(String path, String id, CloudStore cloudStore) {
        try (FileChannel fileChannel = FileChannel.open(Paths.get(path), StandardOpenOption.READ)) {
            cloudStore.upload(id, fileChannel, fileChannel.size());
        } catch (IOException e) {
            die("ERROR: Unable to upload file", e);
        }
    }

    private void downloadFiles(String path, List<String> files, CloudStore cloudStore) {
        logger.info("Started bulk download");

        for (String file : files) {
            downloadFile(path + SystemUtils.FILE_SEPARATOR + file, file, cloudStore);
        }

        logger.info("Finished bulk download");
    }

    private void downloadFile(String path, String id, CloudStore cloudStore) {
        try (FileChannel fileChannel = FileChannel.open(Paths.get(path), StandardOpenOption.WRITE,
             StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
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

        CloudStore cloudStore = context.getBean(CloudStore.class);
        CloudStoreApp app = new CloudStoreApp(cloudStore);

        app.run(args);

        context.close();
    }

}
