package org.avasquez.seccloudfs.processing.db.model;

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
    private byte[] iv;

    /**
     * Private no-arg constructor, for use by frameworks like Jongo/Jackson.
     */
    private EncryptionKey() {
    }

    public EncryptionKey(String dataId, byte[] key, byte[] iv) {
        this.dataId = dataId;
        this.key = key;
        this.iv = iv;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    public byte[] getIv() {
        return iv;
    }

    public void setIv(final byte[] iv) {
        this.iv = iv;
    }

}
