package org.avasquez.seccloudfs.processing.db.model;

import org.apache.shiro.codec.Base64;
import org.jongo.marshall.jackson.oid.Id;
import org.jongo.marshall.jackson.oid.ObjectId;

/**
 * Represents a key used for encryption/decryption.
 *
 * @author avasquez
 */
public class EncryptionKey {

    @Id
    @ObjectId
    private String id;
    private String dataId;
    private byte[] key;

    /**
     * Private no-arg constructor, for use by frameworks like Jongo/Jackson.
     */
    private EncryptionKey() {
    }

    public EncryptionKey(final String dataId, final byte[] key) {
        this.dataId = dataId;
        this.key = key;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(final String dataId) {
        this.dataId = dataId;
    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(final byte[] key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "EncryptionKey{" +
            "id='" + id + '\'' +
            ", dataId='" + dataId + '\'' +
            ", key=" + Base64.encodeToString(key) +
            '}';
    }

}
