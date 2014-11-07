package org.avasquez.seccloudfs.apache.vfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.avasquez.seccloudfs.cloud.CloudStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Apache Commons VFS implementation of {@link org.avasquez.seccloudfs.cloud.CloudStore}.
 *
 * @author avasquez
 */
public class ApacheVfsCloudStore implements CloudStore {

    private static final Logger logger = LoggerFactory.getLogger(ApacheVfsCloudStore.class);

    private static final Pattern URI_WITH_PASSWORD_PATTERN = Pattern.compile("([a-zA-Z]+)://(\\w+):(\\w+)@(.+)");

    private FileSystemManager fileSystemManager;
    private String rootUri;
    private String name;
    
    @PostConstruct
    public void init() {
        Matcher matcher = URI_WITH_PASSWORD_PATTERN.matcher(rootUri);
        if (matcher.matches()) {
            name = matcher.group(1) + "://" + matcher.group(2) + "@" + matcher.group(4);
        } else {
            name = rootUri;
        }        
    }

    @Required
    public void setFileSystemManager(FileSystemManager fileSystemManager) {
        this.fileSystemManager = fileSystemManager;
    }

    @Required
    public void setRootUri(String rootUri) {
        this.rootUri = rootUri;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void upload(String id, ReadableByteChannel src, long length) throws IOException {
        logger.debug("Started uploading {}/{}", name, id);

        FileObject fileObject = getFileObject(id);

        try (OutputStream output = fileObject.getContent().getOutputStream()) {
            IOUtils.copy(Channels.newInputStream(src), output);
        } catch (Exception e) {
            throw new IOException("Error uploading " + rootUri + "/" + id);
        }

        logger.debug("Finished uploading {}/{}", name, id);
    }

    @Override
    public void download(String id, WritableByteChannel target) throws IOException {
        FileObject fileObject = getFileObject(id);
        if (!fileObject.exists()) {
            throw new FileNotFoundException("No file " + name + "/" + id + " found");
        }

        logger.debug("Started downloading {}/{}", name, id);

        try (InputStream input = fileObject.getContent().getInputStream()) {
            IOUtils.copy(input, Channels.newOutputStream(target));
        } catch (Exception e) {
            throw new IOException("Error downloading " + rootUri + "/" + id);
        }

        logger.debug("Finished downloading {}/{}", name, id);
    }

    @Override
    public void delete(String id) throws IOException {
        logger.debug("Deleting {}/{}", name, id);

        try {
            getFileObject(id).delete();
        } catch (Exception e) {
            throw new IOException("Error deleting " + name + "/" + id, e);
        }
    }

    private FileObject getFileObject(String id) throws IOException {
        String finalUri = rootUri + "/" + id;
        try {
            return fileSystemManager.resolveFile(finalUri);
        } catch (Exception e) {
            throw new IOException("Error resolving file object for " + finalUri, e);
        }
    }

}
