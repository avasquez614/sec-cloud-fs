package org.avasquez.seccloudfs.db.mongo;

import com.mongodb.MongoException;

import org.avasquez.seccloudfs.db.Repository;
import org.avasquez.seccloudfs.exception.DbException;
import org.bson.types.ObjectId;
import org.jongo.Jongo;
import org.jongo.MongoCollection;

/**
 * Created by alfonsovasquez on 02/02/14.
 */
public abstract class JongoRepository<T> implements Repository<T> {

    protected MongoCollection collection;

    protected JongoRepository(String collectionName, Jongo jongo) {
        collection = jongo.getCollection(collectionName);
    }

    @Override
    public long count() throws DbException {
        try {
            return collection.count();
        } catch (MongoException e) {
            throw new DbException("[" + collection.getName() + "] Count failed", e);
        }
    }

    @Override
    public Iterable<T> findAll() throws DbException {
        try {
            return collection.find().as(getPojoClass());
        } catch (MongoException e) {
            throw new DbException("[" + collection.getName() + "] Find all failed", e);
        }
    }

    @Override
    public T find(String id) throws DbException {
        try {
            return collection.findOne(new ObjectId(id)).as(getPojoClass());
        } catch (MongoException e) {
            throw new DbException("[" + collection.getName() + "] Find for ID '" + id + "' failed", e);
        }
    }

    @Override
    public void insert(T pojo) throws DbException {
        try {
            collection.insert(pojo);
        } catch (MongoException e) {
            throw new DbException("[" + collection.getName() + "] Insert for " + pojo + " failed", e);
        }
    }

    @Override
    public void save(T pojo) throws DbException {
        try {
            collection.save(pojo);
        } catch (MongoException e) {
            throw new DbException("[" + collection.getName() + "] Save for " + pojo + " failed", e);
        }
    }

    @Override
    public void delete(String id) throws DbException {
        try {
            collection.remove(new ObjectId(id));
        } catch (MongoException e) {
            throw new DbException("[" + collection.getName() + "] Delete for ID '" + id + "' failed", e);
        }
    }

    @Override
    public void deleteAll() throws DbException {
        try {
            collection.remove();
        } catch (MongoException e) {
            throw new DbException("[" + collection.getName() + "] Delete all failed", e);
        }
    }

    public abstract Class<T> getPojoClass();

}
