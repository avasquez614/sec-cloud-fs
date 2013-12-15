package org.avasquez.seccloudfs.filesystem;

import org.avasquez.seccloudfs.filesystem.exception.FileSystemException;

import java.util.List;

/**
 * Interface to the whole virtual filesystem that handles the security and storage of files in the cloud.
 *
 * @author avasquez
 */
public interface FileSystem {

    /**
     * Returns the file for the specified path.
     *
     * @param path  the file's path
     *
     * @return the file if found or null if not
     *
     * @throws FileSystemException if an error occurs
     */
    File getFile(String path) throws FileSystemException;

    /**
     * Create a new file or directory at the specified path.
     *
     * @param path  the path where to create the file
     * @param dir   if a directory should be created instead of a common file
     *
     * @return the created file
     *
     * @throws FileSystemException  if the file already exists, if the parent dir is read-only or if another
     *                              error occurs
     */
    File createFile(String path, boolean dir) throws FileSystemException;

    List<File> getChildren(String path) throws FileSystemException;

    void deleteFile(String path) throws FileSystemException;

    File copyFile(String srcPath, String dstPath) throws FileSystemException;

    File moveFile(String srcPath, String dstPath) throws FileSystemException;

}
