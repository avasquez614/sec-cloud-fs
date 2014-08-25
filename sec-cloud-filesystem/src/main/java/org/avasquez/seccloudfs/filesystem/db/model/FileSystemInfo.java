package org.avasquez.seccloudfs.filesystem.db.model;

import org.jongo.marshall.jackson.oid.Id;
import org.jongo.marshall.jackson.oid.ObjectId;

/**
 * Created by alfonsovasquez on 16/02/14.
 */
public class FileSystemInfo {

    @Id
    @ObjectId
    private String id;
    private String rootDirectory;

    /**
     * Private no-arg constructor, for use by frameworks like Jongo/Jackson.
     */
    private FileSystemInfo() {
    }

    public FileSystemInfo(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public String getId() {
        return id;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    @Override
    public String toString() {
        return "FileSystemInfo{" +
            "id='" + id + '\'' +
            ", rootDirectory='" + rootDirectory + '\'' +
            '}';
    }

}
