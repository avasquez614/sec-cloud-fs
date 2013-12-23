package org.avasquez.seccloudfs.filesystem.impl;

import java.util.Date;
import java.util.List;

/**
 * Log that maintains the information of writes to files.
 *
 * @author avasquez
 */
public interface WriteLog {

    void log(WriteLogEntry entry);

    WriteLogEntry getLatestEntry(String filePath);

    List<WriteLogEntry> getEntriesAfterDate(String filePath, Date date);

}
