package org.github.siahsang.redutils.common.resource;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
public interface ConnectionManager<T> {
    boolean reserve(String id, int size);

    void free(String id);

    boolean reserveOne(String id);

    T borrow(String id);

    void returnBack(String id, T connection);

}
