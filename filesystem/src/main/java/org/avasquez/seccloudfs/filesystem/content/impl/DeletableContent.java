package org.avasquez.seccloudfs.filesystem.content.impl;

import org.avasquez.seccloudfs.filesystem.content.Content;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 19/01/14.
 */
public interface DeletableContent extends Content {

    void delete() throws IOException;

}
