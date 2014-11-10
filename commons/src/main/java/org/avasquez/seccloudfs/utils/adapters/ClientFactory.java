package org.avasquez.seccloudfs.utils.adapters;

/**
 * Implementations create specific clients for different cloud storage backends.
 *
 * @author avasquez
 */
public interface ClientFactory<K, C> {

    K createClient(C credentials);

}
