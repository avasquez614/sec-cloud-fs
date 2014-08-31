package org.avasquez.seccloudfs.gdrive;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.utils.nio.ChannelUtils;
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
    private static final String LIST_FILES_UNDER_FOLDER_QUERY = "'%s' in parents";

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
            // Create root folder if it doesn't exist
            rootFolder = createFolder(rootFolderName);
        } else {
            // Pre-cache existing children
            List<File> files = listFiles(rootFolder.getId());
            for (File file : files) {
                fileCache.put(file.getTitle(), file);
            }
        }
    }

    @Override
    public long upload(String id, SeekableByteChannel src, long length) throws IOException {
        File file = fileCache.get(id);
        InputStreamContent content = new InputStreamContent(BINARY_MIME_TYPE, Channels.newInputStream(src));

        if (file != null) {
            logger.debug("File '{}' already exists in {} store. Updating it...", id, name);

            try {
                file = drive.files().update(file.getId(), file, content).execute();
            } catch (IOException e) {
                throw new IOException("Error updating file '" + id + "' in store " + name, e);
            }
        } else {
            logger.debug("File '{}' doesn't exist in {} store. Inserting it...", id, name);

            file = new File();
            file.setTitle(id);
            file.setParents(Arrays.asList((new ParentReference()).setId(rootFolder.getId())));

            try {
                file = drive.files().insert(file, content).execute();
            } catch (IOException e) {
                throw new IOException("Error inserting file '" + id + "' in store " + name, e);
            }

            fileCache.put(id, file);
        }

        return file.getFileSize();
    }

    @Override
    public long download(String id, SeekableByteChannel target) throws IOException {
        File file = fileCache.get(id);

        if (file != null && StringUtils.isNotEmpty(file.getDownloadUrl())) {
            logger.debug("Downloading file {} from store {}", id, name);

            try {
                HttpRequest request = drive.getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl()));
                HttpResponse response = request.execute();
                ReadableByteChannel content = Channels.newChannel(response.getContent());

                return ChannelUtils.copy(content, target);
            } catch (IOException e) {
                throw new IOException("Error downloading file '" + id + "' from store " + name, e);
            }
        } else {
            return 0;
        }
    }

    @Override
    public void delete(String id) throws IOException {
        File file = fileCache.get(id);

        if (file != null) {
            logger.debug("Deleting {}/{} from store {}", rootFolderName, id, name);

            try {
                drive.files().delete(file.getId()).execute();
            } catch (IOException e) {
                throw new IOException("Error deleting file '" + id + "' from store " + name, e);
            }

            fileCache.remove(id);
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
                throw new IllegalStateException("More than one root folder with name '" + rootFolderName +
                    "' found in store " + name);
            }
        } catch (IOException e) {
            throw new IOException("Error retrieving root folder '" + rootFolderName + "' from store " + name, e);
        }
    }

    private File createFolder(String folderName) throws IOException {
        File folder = new File();
        folder.setTitle(folderName);
        folder.setMimeType(FOLDER_MIME_TYPE);

        try {
            folder = drive.files().insert(folder).execute();
        } catch (IOException e) {
            throw new IOException("Error creating folder '" + folderName + "' in store " + name, e);
        }

        return folder;
    }

    private List<File> listFiles(String folderId) throws IOException {
        try {
            List<File> result = new ArrayList<>();
            String query = String.format(LIST_FILES_UNDER_FOLDER_QUERY, folderId);
            Drive.Files.List request = drive.files().list().setQ(query);

            do {
                FileList files = request.execute();
                result.addAll(files.getItems());

                request.setPageToken(files.getNextPageToken());
            } while (StringUtils.isNotEmpty(request.getPageToken()));

            return result;
        } catch (IOException e) {
            throw new IOException("Error listing files for folder '" + folderId + "' in store " + name, e);
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