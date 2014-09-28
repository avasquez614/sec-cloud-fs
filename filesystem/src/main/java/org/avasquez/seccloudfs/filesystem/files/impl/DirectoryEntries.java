package org.avasquez.seccloudfs.filesystem.files.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.ListUtils;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.model.DirectoryEntry;
import org.avasquez.seccloudfs.filesystem.db.repos.DirectoryEntryRepository;
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

        Iterable<DirectoryEntry> entries;
        try {
            entries = entryRepo.findByDirectoryId(directoryId);
        } catch (DbException e) {
            throw new IOException("Unable to retrieve dir entries for dir ID '" + directoryId + "'", e);
        }

        if (entries != null) {
            entries = deleteDuplicateEntries(IteratorUtils.toList(entries.iterator()));
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
        List<DirectoryEntry> finalEntries = new ArrayList<>();

        for (DirectoryEntry entry : entries) {
            int idx = indexOfDuplicateEntry(finalEntries, entry);
            if (idx >= 0) {
                DirectoryEntry duplicateEntry = finalEntries.get(idx);
                if (entry.getAddedDate().after(duplicateEntry.getAddedDate())) {
                    finalEntries.set(idx, entry);
                }
            } else {
                finalEntries.add(entry);
            }
        }

        List<DirectoryEntry> duplicateEntries = ListUtils.removeAll(entries, finalEntries);

        if (!duplicateEntries.isEmpty()) {
            logger.debug("Duplicate directory entries found: " + duplicateEntries);
        }

        for (DirectoryEntry duplicateEntry : duplicateEntries) {
            try {
                entryRepo.delete(duplicateEntry.getId());
            } catch (DbException e) {
                throw new IOException("Unable to delete " + duplicateEntry + " from DB", e);
            }
        }

        return finalEntries;
    }

    private int indexOfDuplicateEntry(List<DirectoryEntry> entries, DirectoryEntry entry) {
        for (ListIterator<DirectoryEntry> iter = entries.listIterator(); iter.hasNext();) {
            int idx = iter.nextIndex();
            if (duplicateEntries(iter.next(), entry)) {
                return idx;
            }
        }

        return -1;
    }

    private boolean duplicateEntries(DirectoryEntry entry1, DirectoryEntry entry2) {
        return !entry1.equals(entry2) && entry1.getFileName().equals(entry2.getFileName());
    }

}
