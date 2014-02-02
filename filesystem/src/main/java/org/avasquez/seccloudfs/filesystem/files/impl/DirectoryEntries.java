package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.filesystem.db.dao.DirectoryEntryDao;
import org.avasquez.seccloudfs.filesystem.db.model.DirectoryEntry;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by alfonsovasquez on 01/02/14.
 */
public class DirectoryEntries {

    private DirectoryEntryDao entryDao;
    private String directoryId;
    private Map<String, DirectoryEntry> entries;

    public DirectoryEntries(DirectoryEntryDao entryDao, String directoryId) {
        this.entryDao = entryDao;
        this.directoryId = directoryId;
        this.entries = new ConcurrentHashMap<>();

        Iterable<DirectoryEntry> entries = entryDao.findByDirectoryId(directoryId);
        if (entries != null) {
            for (DirectoryEntry entry : entries) {
                this.entries.put(entry.getFileName(), entry);
            }
        }
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean hasEntry(String fileName) {
        return entries.containsKey(fileName);
    }

    public DirectoryEntry getEntry(String fileName) {
        return entries.get(fileName);
    }

    public String[] getFileNames() {
        Set<String> fileNames = entries.keySet();

        return fileNames.toArray(new String[fileNames.size()]);
    }

    public DirectoryEntry createEntry(String fileName, String fileId) {
        DirectoryEntry entry = new DirectoryEntry(directoryId, fileName, fileId);

        entryDao.insert(entry);

        entries.put(fileName, entry);

        return entry;
    }

    public void moveEntryTo(String fileName, DirectoryEntries dst, String newFileName) {
        DirectoryEntry entry = getEntry(fileName);
        if (entry != null) {
            DirectoryEntry replacedEntry = dst.entries.get(fileName);
            DirectoryEntry movedEntry = new DirectoryEntry(entry.getId(),
                    dst.directoryId,
                    newFileName,
                    entry.getFileId());

            entryDao.save(movedEntry);

            entries.remove(fileName);
            dst.entries.put(newFileName, movedEntry);

            if (replacedEntry != null) {
                entryDao.delete(replacedEntry.getId());
            }
        }
    }

    public void deleteEntry(String fileName) {
        DirectoryEntry entry = getEntry(fileName);
        if (entry != null) {
            entryDao.delete(entry.getId());

            entries.remove(fileName);
        }
    }

    @Override
    public String toString() {
        return "DirectoryEntries{" +
                "directoryId='" + directoryId + '\'' +
                ", entries=" + entries +
                '}';
    }

}
