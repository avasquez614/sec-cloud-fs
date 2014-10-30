package org.avasquez.seccloudfs.db.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.avasquez.seccloudfs.db.Repository;
import org.avasquez.seccloudfs.exception.DbException;
import org.springframework.beans.factory.annotation.Required;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link org.avasquez.seccloudfs.db.Repository} that stores each POJO in a directory in its
 * own file.
 *
 * @author avasquez
 */
public abstract class FileRepository<T> implements Repository<T> {

    private File credentialsDirectory;
    private ObjectMapper jsonMapper;

    public FileRepository(File credentialsDirectory) {
        this(credentialsDirectory, new ObjectMapper());
    }

    public FileRepository(File credentialsDirectory, ObjectMapper jsonMapper) {
        this.credentialsDirectory = credentialsDirectory;
        this.jsonMapper = jsonMapper;

        if (!credentialsDirectory.exists()) {
            credentialsDirectory.mkdirs();
        }
    }

    @Override
    public long count() throws DbException {
        return listFiles().length;
    }

    @Override
    public Iterable<T> findAll() throws DbException {
        List<T> pojos = new ArrayList<>();

        for (File file : listFiles()) {
            pojos.add(deserializeFile(file));
        }

        return pojos;
    }

    @Override
    public T find(String id) throws DbException {
        File file = getFile(id);
        if (file.exists()) {
            return deserializeFile(file);
        } else {
            return null;
        }
    }

    @Override
    public void insert(T pojo) throws DbException {
        String id = generateId();

        setPojoId(id, pojo);
        serializeFile(getFile(id), pojo);
    }

    @Override
    public void save(T pojo) throws DbException {
        serializeFile(getFile(pojo), pojo);
    }

    @Override
    public void delete(String id) throws DbException {
        getFile(id).delete();
    }

    @Override
    public void deleteAll() throws DbException {
        for (File file : listFiles()) {
            file.delete();
        }
    }

    public abstract Class<T> getPojoClass();

    public abstract String getPojoFilenameExtension();

    public abstract String getPojoId(T pojo);

    public abstract void setPojoId(String id, T pojo);

    private File[] listFiles() {
        return credentialsDirectory.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(getPojoFilenameExtension());
            }

        });
    }

    private T deserializeFile(File file) throws DbException {
        try {
            return jsonMapper.readValue(file, getPojoClass());
        } catch (IOException e) {
            throw new DbException("Error deserializing file " + file, e);
        }
    }

    private void serializeFile(File file, T pojo) throws DbException {
        try {
            jsonMapper.writeValue(file, pojo);
        } catch (IOException e) {
            throw new DbException("Error serializing POJO " + pojo + " to file " + file, e);
        }
    }

    private String generateId() {
        return UUID.randomUUID().toString();
    }

    private File getFile(String id) {
        return new File(credentialsDirectory, id + getPojoFilenameExtension());
    }

    private File getFile(T pojo) {
        String id = getPojoId(pojo);

        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("No ID found for POJO " + pojo);
        }

        return getFile(id);
    }

}
