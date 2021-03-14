package org.github.siahsang.redutils.common.connection;

import redis.clients.jedis.Jedis;

import java.util.function.Function;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
public interface ConnectionManager<T> {
    boolean reserve(String id, int size);

    void free(String id);

    void free();

    boolean reserve(int size);

    boolean reserveOne();

    T borrow(String id);

    T borrow();

    void returnBack(String id, T connection);

    void returnBack(T connection);

    <E> E doWithConnection(String id, Function<Jedis, E> operation);

    <E> E doWithConnection(Function<Jedis, E> operation);


}
