package org.avasquez.seccloudfs.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alfonsovasquez on 02/02/14.
 */
public class CollectionUtils {

    private CollectionUtils() {
    }

    public static <T> List<T> asList(Iterable<T> iterable) {
        if (iterable != null) {
            List<T> list = new ArrayList<>();

            for (T elem : iterable) {
                list.add(elem);
            }

            return list;
        } else {
            return null;
        }
    }

}
