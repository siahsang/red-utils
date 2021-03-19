package org.github.siahsang.redutils.common.connection;

import redis.clients.jedis.Jedis;

import java.util.function.Function;

/**
 * @author Javad Alimohammadi
 */
public interface ConnectionManager<T> {
    boolean reserve(String resourceId, int size);

    /**
     * First close all connection and then remove the resource id
     * @param resourceId Client id that request for getting connection
     */
    void free(String resourceId);


    void free();

    boolean reserve(int size);

    boolean reserveOne();

    T borrow(String resourceId);

    T borrow();

    void returnBack(String resourceId, T connection);

    void returnBack(T connection);

    <E> E doWithConnection(String resourceId, Function<Jedis, E> operation);

    <E> E doWithConnection(Function<Jedis, E> operation);

    int remainingCapacity();
}
