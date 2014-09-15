package org.avasquez.seccloudfs.gdrive;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.avasquez.seccloudfs.cloud.CloudStore;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Google Drive implementation of {@link org.avasquez.seccloudfs.cloud.CloudStore}.
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
    private Cache<String, File> fileCache;
    private String rootFolderName;

    private File rootFolder;

    public GoogleDriveCloudStore(String name, Drive drive, Cache<String, File> fileCache, String rootFolderName) {
        this.name = name;
        this.drive = drive;
        this.fileCache = fileCache;
        this.rootFolderName = rootFolderName;
    }

    @Override
    public String getName() {
        return name;
    }

    public void init() throws IOException {
        rootFolder = getRootFolder();
        if (rootFolder == null) {
            logger.debug("Root folder '{}' doesn't exist in store {}. Creating it...", rootFolderName, name);

            // Create root folder if it doesn't exist
            rootFolder = createRootFolder();
        }
    }

    @Override
    public void upload(String filename, ReadableByteChannel src, long length) throws IOException {
        InputStreamContent content = new InputStreamContent(BINARY_MIME_TYPE, Channels.newInputStream(src));
        content.setLength(length);

        File file = getCachedFile(filename);

        if (file != null) {
            logger.debug("File {}/{}/{} already exists. Updating it...", name, rootFolderName, filename);

            try {
                drive.files().update(file.getId(), file, content).execute();
            } catch (IOException e) {
                throw new IOException("Error updating file " + name + "/" + rootFolderName + "/" + filename, e);
            }
        } else {
            logger.debug("File {}/{}/{} doesn't exist. Inserting it...", name, rootFolderName, filename);

            file = new File();
            file.setTitle(filename);
            file.setParents(Arrays.asList((new ParentReference()).setId(rootFolder.getId())));

            try {
                file = drive.files().insert(file, content).execute();
            } catch (Exception e) {
                throw new IOException("Error inserting " + name + "/" + rootFolderName + "/" + filename, e);
            }

            fileCache.put(filename, file);
        }

        logger.debug("Finished uploading {}/{}/{}", name, rootFolderName, filename);
    }

    @Override
    public void download(String filename, WritableByteChannel target) throws IOException {
        File file = getCachedFile(filename);

        if (file != null) {
            logger.debug("Started downloading {}/{}/{}", name, rootFolderName, filename);

            try {
                HttpRequest request = drive.getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl()));
                HttpResponse response = request.execute();

                try (InputStream in = response.getContent()) {
                    IOUtils.copy(in, Channels.newOutputStream(target));
                }
            } catch (Exception e) {
                throw new IOException("Error downloading " + name + "/" + rootFolderName + "/" + filename, e);
            }

            logger.debug("Finished downloading {}/{}/{}", name, rootFolderName, filename);
        } else {
            throw new FileNotFoundException("No file " + name + "/" + rootFolderName + "/" + filename + " found");
        }
    }

    @Override
    public void delete(String filename) throws IOException {
        File file = getCachedFile(filename);

        if (file != null) {
            logger.debug("Deleting {}/{}/{}", name, rootFolderName, filename);

            try {
                drive.files().delete(file.getId()).execute();
            } catch (Exception e) {
                throw new IOException("Error deleting " + name + "/" + rootFolderName + "/" + filename, e);
            }

            fileCache.remove(filename);
        }
    }

    @Override
    public long getTotalSpace() throws IOException {
        return getAbout().getQuotaBytesTotal();
    }

    @Override
    public long getAvailableSpace() throws IOException {
        About about = getAbout();
        long total = about.getQuotaBytesTotal();
        long used = about.getQuotaBytesUsed();

        return total - used;
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

    private About getAbout() throws IOException {
        try {
            return drive.about().get().execute();
        } catch (IOException e) {
            throw new IOException("Error retrieving Google Drive account info for store " + name, e);
        }
    }

}
