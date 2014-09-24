package org.avasquez.seccloudfs.utils;

/**
 * Factory that wraps an object with decorators.
 *
 * @author avasquez
 */
public interface DecoratorFactory<T> {

    /**
     * Wraps the specified object with any number of decorators.
     *
     * @param obj the object to decorate
     *
     * @return the decorated object
     */
    T decorate(T obj);

}
