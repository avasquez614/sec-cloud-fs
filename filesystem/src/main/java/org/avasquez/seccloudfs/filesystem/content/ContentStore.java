package org.avasquez.seccloudfs.filesystem.content;

/**
 * Created by alfonsovasquez on 09/01/14.
 */
public interface ContentStore {

    Content find(String id);

    Content create();

    void delete(String id);

}
