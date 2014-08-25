package org.avasquez.seccloudfs.gdrive.utils.store;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredential;
import org.avasquez.seccloudfs.gdrive.db.repos.GoogleDriveCredentialRepository;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link com.google.api.client.util.store.DataStore} for {@link com.google.api.client.auth.oauth2.StoredCredential}s
 * where the credentials are stored as {@link org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredential}s in a DB.
 *
 * @author avasquez
 */
public class StoredCredentialDatabaseDataStore implements DataStore<StoredCredential> {

    private GoogleDriveCredentialRepository repository;

    @Required
    public void setRepository(final GoogleDriveCredentialRepository repository) {
        this.repository = repository;
    }

    @Override
    public DataStoreFactory getDataStoreFactory() {
        return null;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public int size() throws IOException {
        try {
            return (int) repository.count();
        } catch (DbException e) {
            throw new IOException("Unable to count all credentials", e);
        }
    }

    @Override
    public boolean isEmpty() throws IOException {
        return size() == 0;
    }

    @Override
    public boolean containsKey(final String key) throws IOException {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(final StoredCredential value) throws IOException {
        return values().contains(value);
    }

    @Override
    public Set<String> keySet() throws IOException {
        Iterable<GoogleDriveCredential> credentials;
        try {
            credentials = repository.findAll();
        } catch (DbException e) {
            throw new IOException("Unable to find all credentials", e);
        }

        Set<String> userIds = new HashSet<>();
        for (GoogleDriveCredential credential : credentials) {
            userIds.add(credential.getUserId());
        }

        return userIds;
    }

    @Override
    public Collection<StoredCredential> values() throws IOException {
        Iterable<GoogleDriveCredential> credentials;
        try {
            credentials = repository.findAll();
        } catch (DbException e) {
            throw new IOException("Unable to find all credentials", e);
        }

        List<StoredCredential> storedCredentials = new ArrayList<>();
        for (GoogleDriveCredential credential : credentials) {
            StoredCredential storedCredential = new StoredCredential();
            storedCredential.setAccessToken(credential.getAccessToken());
            storedCredential.setRefreshToken(credential.getRefreshToken());
            storedCredential.setExpirationTimeMilliseconds(credential.getExpirationTime());

            storedCredential.equals(storedCredential);
        }

        return storedCredentials;
    }

    @Override
    public StoredCredential get(final String key) throws IOException {
        GoogleDriveCredential credential;
        try {
            credential = repository.findByUserId(key);
        } catch (DbException e) {
            throw new IOException("Unable to find credential by user ID '" + key + "'", e);
        }

        StoredCredential storedCredential = new StoredCredential();
        storedCredential.setAccessToken(credential.getAccessToken());
        storedCredential.setRefreshToken(credential.getRefreshToken());
        storedCredential.setExpirationTimeMilliseconds(credential.getExpirationTime());

        return storedCredential;
    }

    @Override
    public DataStore<StoredCredential> set(final String key, final StoredCredential value) throws IOException {
        try {
            repository.save(new GoogleDriveCredential(key, value.getAccessToken(), value.getRefreshToken(),
                value.getExpirationTimeMilliseconds()));
        } catch (DbException e) {
            throw new IOException("Unable to save credential for user ID '" + key + "'", e);
        }

        return this;
    }

    @Override
    public DataStore<StoredCredential> clear() throws IOException {
        try {
            repository.deleteAll();
        } catch (DbException e) {
            throw new IOException("Unable to delete all credentials", e);
        }

        return this;
    }

    @Override
    public DataStore<StoredCredential> delete(final String key) throws IOException {
        try {
            repository.delete(key);
        } catch (DbException e) {
            throw new IOException("Unable to delete credential for user ID '" + key + "'", e);
        }

        return this;
    }

}
