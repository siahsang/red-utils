package org.github.siahsang.redutils.common.connection;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.github.siahsang.redutils.common.RedUtilsConfig;
import org.github.siahsang.redutils.common.ThreadManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
public class JedisConnectionManager implements ConnectionManager<Jedis> {
    private final AtomicInteger maximumAllowedConnection = new AtomicInteger(0);

    private final Map<String, List<Jedis>> reservedConnections = new ConcurrentHashMap<>();


    private final JedisPool channelConnectionPool;

    private final ThreadManager threadManager = new ThreadManager();


    public JedisConnectionManager(RedUtilsConfig redUtilsConfig) {
        this.maximumAllowedConnection.set(redUtilsConfig.getLockMaxPoolSize());

        GenericObjectPoolConfig<Jedis> lockPoolConfig = ConnectionPoolFactory.makePool(maximumAllowedConnection.get());

        this.channelConnectionPool = new JedisPool(lockPoolConfig,
                redUtilsConfig.getHostAddress(),
                redUtilsConfig.getPort(),
                redUtilsConfig.getReadTimeOutMillis()
        );
    }

    @Override
    public boolean reserve(final String id, final int size) {
        AtomicBoolean reservedSuccessfully = new AtomicBoolean(false);
        maximumAllowedConnection.updateAndGet(operand -> {
            if (operand - size >= 0) {
                reservedSuccessfully.set(true);
                return operand - size;
            } else {
                reservedSuccessfully.set(false);
                return operand;
            }
        });

        if (reservedSuccessfully.get()) {
            reservedConnections.putIfAbsent(id, new ArrayList<>());
            for (int i = 0; i < size; i++) {
                Jedis resource = channelConnectionPool.getResource();
                reservedConnections.get(id).add(resource);
            }
        }

        return reservedSuccessfully.get();
    }

    @Override
    public boolean reserve(final int size) {
        String connectionId = threadManager.generateUniqueValue();
        return reserve(connectionId, size);
    }


    @Override
    public boolean reserveOne(final String id) {
        return reserve(id, 1);
    }

    @Override
    public boolean reserveOne() {
        String connectionId = threadManager.generateUniqueValue();
        return reserve(connectionId, 1);
    }

    @Override
    public Jedis borrow(final String id) {
        List<Jedis> returnList = new ArrayList<>();
        reservedConnections.compute(id, (s, jedisList) -> {
            if (Objects.isNull(jedisList)) {
                throw new RuntimeException("First you should reserve connection");
            }

            if (jedisList.isEmpty()) {
                throw new RuntimeException("There is no any connection. Reserve it!");
            }

            Jedis jedis = jedisList.remove(jedisList.size() - 1);
            returnList.add(jedis);

            return jedisList;
        });

        return returnList.get(0);
    }


    @Override
    public Jedis borrow() {
        String connectionId = threadManager.generateUniqueValue();
        return borrow(connectionId);
    }

    @Override
    public void returnBack(final String id, final Jedis connection) {
        reservedConnections.compute(id, (s, jedisList) -> {
            if (Objects.isNull(jedisList)) {
                throw new RuntimeException("There is no id " + id);
            }

            jedisList.add(connection);

            return jedisList;
        });
    }


    @Override
    public <E> E doWithConnection(String id, Function<Jedis, E> operation) {
        try (Jedis jedis = borrow(id)) {
            return operation.apply(jedis);
        }
    }

    @Override
    public <E> E doWithConnection(Function<Jedis, E> operation) {
        String connectionId = threadManager.generateUniqueValue();
        return doWithConnection(connectionId, operation);
    }


    @Override
    public void free(String id) {
        reservedConnections.compute(id, (s, jedisList) -> {
            if (Objects.isNull(jedisList)) {
                throw new RuntimeException("There is no id " + id);
            }

            if (!jedisList.isEmpty()) {
                jedisList.forEach(Jedis::close);
            }

            jedisList.clear();

            return null;
        });
    }

    @Override
    public void free() {
        String connectionId = threadManager.generateUniqueValue();
        free(connectionId);
    }
}
