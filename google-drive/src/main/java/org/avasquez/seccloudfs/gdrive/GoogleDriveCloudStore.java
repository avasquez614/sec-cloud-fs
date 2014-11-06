package org.avasquez.seccloudfs.gdrive;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;
import org.apache.commons.io.IOUtils;
import org.avasquez.seccloudfs.cloud.CloudStore;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.List;

/**
 * Google Drive implementation of {@link org.avasquez.seccloudfs.cloud.CloudStore}. This cloud store is synchronized
 * because Google Drive doesn't seem to like when a directory's contents are being modified concurrently.
 *
 * @author avasquez
 */
public class GoogleDriveCloudStore implements CloudStore {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveCloudStore.class);

    private static final String BINARY_MIME_TYPE = "application/octet-stream";
    private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

    private static final String ROOT_FOLDER_QUERY = "mimeType = '" + FOLDER_MIME_TYPE + "' and title = '%s'";
    private static final String FIND_FILE_QUERY = "'%s' in parents and title = '%s'";

    private String name;
    private Drive drive;
    private String rootFolderName;
    private long chunkedUploadThreshold;
    private Cache<String, File> fileCache;

    private File rootFolder;

    public GoogleDriveCloudStore(String name, Drive drive, String rootFolderName, long chunkedUploadThreshold,
                                 Cache<String, File> fileCache) {
        this.name = name;
        this.drive = drive;
        this.fileCache = fileCache;
        this.chunkedUploadThreshold = chunkedUploadThreshold;
        this.rootFolderName = rootFolderName;
    }

    @Override
    public String getName() {
        return name;
    }

    @PostConstruct
    public void init() throws IOException {
        rootFolder = getRootFolder();
        if (rootFolder == null) {
            logger.info("Root folder '{}' doesn't exist in store {}. Creating it...", rootFolderName, name);

            // Create root folder if it doesn't exist
            rootFolder = createRootFolder();
        }
    }

    @Override
    public synchronized void upload(String filename, ReadableByteChannel src, long length) throws IOException {
        InputStreamContent content = new InputStreamContent(BINARY_MIME_TYPE, Channels.newInputStream(src));
        content.setLength(length);

        File file = getCachedFile(filename);

        if (file != null) {
            logger.debug("File {}/{} already exists. Updating it...", name, filename);

            try {
                executeUpload(drive.files().update(file.getId(), file, content), filename, length);
            } catch (IOException e) {
                throw new IOException("Error updating file " + name + "/" + filename, e);
            }
        } else {
            logger.debug("File {}/{} doesn't exist. Inserting it...", name, filename);

            file = new File();
            file.setTitle(filename);
            file.setParents(Arrays.asList((new ParentReference()).setId(rootFolder.getId())));

            try {
                file = executeUpload(drive.files().insert(file, content), filename, length);
            } catch (Exception e) {
                throw new IOException("Error inserting " + name + "/" + filename, e);
            }

            fileCache.put(filename, file);
        }

        logger.debug("Finished uploading {}/{}", name, filename);
    }

    @Override
    public synchronized void download(String filename, WritableByteChannel target) throws IOException {
        File file = getCachedFile(filename);

        if (file != null) {
            logger.debug("Started downloading {}/{}", name, filename);

            try {
                HttpRequest request = drive.getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl()));
                HttpResponse response = request.execute();

                try (InputStream in = response.getContent()) {
                    IOUtils.copy(in, Channels.newOutputStream(target));
                }
            } catch (Exception e) {
                throw new IOException("Error downloading " + name + "/" + filename, e);
            }

            logger.debug("Finished downloading {}/{}", name, filename);
        } else {
            throw new FileNotFoundException("No file " + name + "/" + filename + " found");
        }
    }

    @Override
    public synchronized void delete(String filename) throws IOException {
        File file = getCachedFile(filename);

        if (file != null) {
            logger.debug("Deleting {}/{}", name, filename);

            try {
                drive.files().delete(file.getId()).execute();
            } catch (Exception e) {
                throw new IOException("Error deleting " + name + "/" + filename, e);
            }

            fileCache.remove(filename);
        }
    }

    private File getRootFolder() throws IOException, IllegalArgumentException {
        try {
            String query = String.format(ROOT_FOLDER_QUERY, rootFolderName);
            Drive.Files.List request = drive.files().list().setQ(query);

            List<File> files = request.execute().getItems();
            if (files.size() == 0) {
                return null;
            } else if (files.size() == 1) {
                return files.get(0);
            } else {
                throw new IllegalStateException("Multiple '" + rootFolderName + "' folders found in store " + name);
            }
        } catch (IOException e) {
            throw new IOException("Error retrieving root folder '" + rootFolderName + "' from store " + name, e);
        }
    }

    private File createRootFolder() throws IOException {
        File folder = new File();
        folder.setTitle(rootFolderName);
        folder.setMimeType(FOLDER_MIME_TYPE);

        try {
            folder = drive.files().insert(folder).execute();

            logger.debug("Root folder '" + rootFolderName + "' created in store " + name);
        } catch (IOException e) {
            throw new IOException("Error creating root folder '" + rootFolderName + "' in store " + name, e);
        }

        return folder;
    }

    private File getCachedFile(String filename) throws IOException {
        File file = fileCache.get(filename);
        if (file == null) {
            file = findFile(filename);
            if (file != null) {
                fileCache.put(filename, file);
            }
        }

        return file;
    }

    private File findFile(String filename) throws IOException, IllegalArgumentException {
        try {
            String query = String.format(FIND_FILE_QUERY, rootFolder.getId(), filename);
            Drive.Files.List request = drive.files().list().setQ(query);

            List<File> files = request.execute().getItems();
            if (files.size() == 0) {
                return null;
            } else if (files.size() == 1) {
                return files.get(0);
            } else {
                throw new IllegalStateException("Multiple '" + filename + "' files found in store " + name);
            }
        } catch (IOException e) {
            throw new IOException("Error finding file '" + filename + "' in store " + name, e);
        }
    }

    private File executeUpload(AbstractGoogleClientRequest<File> uploadRequest, String filename,
                               long length) throws IOException {
        MediaHttpUploader uploader = uploadRequest.getMediaHttpUploader();
        uploader.setDisableGZipContent(true);

        if (length < chunkedUploadThreshold) {
            logger.debug("Using direct upload for {}/{}", name, filename);

            uploader.setDirectUploadEnabled(true);
        } else {
            logger.debug("Using chunked upload for {}/{}", name, filename);

            uploader.setDirectUploadEnabled(false);
        }

        return uploadRequest.execute();
    }

}
