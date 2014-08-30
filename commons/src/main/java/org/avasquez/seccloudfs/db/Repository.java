package org.avasquez.seccloudfs.db;

import org.avasquez.seccloudfs.exception.DbException;

/**
 * Common interface for DB CRUD repositories.
 *
 * @author avasquez
 */
public interface Repository<T> {

    /**
     * Returns the number of elements in the DB for the repository.
     *
     * @return the number of elements
     */
    long count() throws DbException;

    /**
     * Returns all elements in the DB for the repository.
     *
     * @return all elements for the repository
     */
    Iterable<T> findAll() throws DbException;

    /**
     * Returns the POJO with the specified ID in the database.
     *
     * @param id  the ID of the POJO to look for
     *
     * @return the POJO, or null if not found
     */
    T find(String id) throws DbException;

    /**
     * Inserts the specified POJO in the database.
     *
     * @param pojo the POJO to insert
     */
    void insert(T pojo) throws DbException;

    /**
     * Saves the specified POJO in the database.
     *
     * @param pojo the POJO to update
     */
    void save(T pojo) throws DbException;

    /**
     * Deletes the POJO for the specified ID in the database.
     *
     * @param id the ID of the POJO to delete
     */
    void delete(String id) throws DbException;

    /**
     * Deletes all elements in the DB for the repository.
     */
    void deleteAll() throws DbException;

}
