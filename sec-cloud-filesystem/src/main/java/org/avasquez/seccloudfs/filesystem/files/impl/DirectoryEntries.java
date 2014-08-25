package org.avasquez.seccloudfs.filesystem.files.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.model.DirectoryEntry;
import org.avasquez.seccloudfs.filesystem.db.repos.DirectoryEntryRepository;
import org.avasquez.seccloudfs.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by alfonsovasquez on 01/02/14.
 */
public class DirectoryEntries {

    private static final Logger logger = LoggerFactory.getLogger(DirectoryEntries.class);

    private DirectoryEntryRepository entryRepo;
    private String directoryId;
    private Map<String, DirectoryEntry> entries;

    public DirectoryEntries(DirectoryEntryRepository entryRepo, String directoryId) throws IOException {
        this.entryRepo = entryRepo;
        this.directoryId = directoryId;
        this.entries = new ConcurrentHashMap<>();

        List<DirectoryEntry> entries;
        try {
            entries = CollectionUtils.asList(entryRepo.findByDirectoryId(directoryId));
        } catch (DbException e) {
            throw new IOException("Unable to retrieve dir entries for dir ID '" + directoryId + "'", e);
        }

        if (entries != null) {
            entries = deleteDuplicateEntries(entries);
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

    public DirectoryEntry createEntry(String fileName, String fileId) throws IOException {
        DirectoryEntry entry = new DirectoryEntry(directoryId, fileName, fileId, new Date());

        try {
            entryRepo.insert(entry);
        } catch (DbException e) {
            throw new IOException("Unable to insert " + entry + " into DB", e);
        }

        entries.put(fileName, entry);

        logger.debug("{} created", entry);

        return entry;
    }

    public void moveEntryTo(String fileName, DirectoryEntries dst, String newFileName) throws IOException {
        DirectoryEntry entry = getEntry(fileName);
        if (entry != null) {
            DirectoryEntry replacedEntry = dst.entries.get(newFileName);
            DirectoryEntry movedEntry = new DirectoryEntry(entry.getId(),
                    dst.directoryId,
                    newFileName,
                    entry.getFileId(),
                    new Date());

            try {
                entryRepo.save(movedEntry);
            } catch (DbException e) {
                throw new IOException("Unable to save " + movedEntry + " in DB", e);
            }

            entries.remove(fileName);
            dst.entries.put(newFileName, movedEntry);

            logger.debug("{} moved from {} with name '{}'", movedEntry, this, fileName);

            if (replacedEntry != null) {
                try {
                    entryRepo.delete(replacedEntry.getId());
                } catch (DbException e) {
                    throw new IOException("Unable to delete entry " + replacedEntry + " from DB", e);
                }

                logger.debug("{} deleted (replaced by entry {})", replacedEntry, movedEntry);
            }
        }
    }

    public void deleteEntry(String fileName) throws IOException {
        DirectoryEntry entry = getEntry(fileName);
        if (entry != null) {
            try {
                entryRepo.delete(entry.getId());
            } catch (DbException e) {
                throw new IOException("Unable to delete entry " + entry + " from DB", e);
            }

            entries.remove(fileName);

            logger.debug("{} deleted", entry);
        }
    }

    @Override
    public String toString() {
        return "DirectoryEntries{" +
                "directoryId='" + directoryId + '\'' +
                ", entries=" + entries +
                '}';
    }

    private List<DirectoryEntry> deleteDuplicateEntries(List<DirectoryEntry> entries) throws IOException {
        List<DirectoryEntry> nonDupEntries = new ArrayList<>();
        List<DirectoryEntry> dupEntries = new ArrayList<>();

        for (DirectoryEntry entry : entries) {
            if (!hasEntryWithSameName(nonDupEntries, entry)) {
                DirectoryEntry dupEntry = getDuplicateEntry(entries, entry);
                if (dupEntry != null) {
                    if (dupEntry.getAddedDate().after(entry.getAddedDate())) {
                        nonDupEntries.add(dupEntry);
                        dupEntries.add(entry);
                    } else {
                        nonDupEntries.add(entry);
                        dupEntries.add(dupEntry);
                    }
                } else {
                    nonDupEntries.add(entry);
                }
            }
        }

        for (DirectoryEntry dupEntry : dupEntries) {
            try {
                entryRepo.delete(dupEntry.getId());
            } catch (DbException e) {
                throw new IOException("Unable to delete " + dupEntry + " from DB", e);
            }
        }

        return nonDupEntries;
    }

    private boolean hasEntryWithSameName(List<DirectoryEntry> entries, DirectoryEntry entry) {
        for (DirectoryEntry ent : entries) {
            if (ent.getFileName().equals(entry.getFileName())) {
                return true;
            }
        }

        return false;
    }

    private DirectoryEntry getDuplicateEntry(List<DirectoryEntry> entries, DirectoryEntry entry) {
        for (DirectoryEntry ent : entries) {
            if (ent.getFileName().equals(entry.getFileName())) {
                return ent;
            }
        }

        return null;
    }

}
