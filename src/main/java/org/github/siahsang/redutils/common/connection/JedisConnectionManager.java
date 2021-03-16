package org.github.siahsang.redutils.common.connection;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.github.siahsang.redutils.common.RedUtilsConfig;
import org.github.siahsang.redutils.common.ThreadManager;
import org.github.siahsang.redutils.exception.ConnectionManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(JedisConnectionManager.class);

    private final AtomicInteger maximumAllowedConnection = new AtomicInteger(0);

    private final Map<String, List<Jedis>> reservedConnections = new ConcurrentHashMap<>();


    private final JedisPool channelConnectionPool;

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
    public boolean reserve(final String resourceId, final int size) {
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
            reservedConnections.putIfAbsent(resourceId, new ArrayList<>());
            for (int i = 0; i < size; i++) {
                Jedis resource = channelConnectionPool.getResource();
                reservedConnections.get(resourceId).add(resource);
                log.debug("Reserved connection with resource_id [{}] successfully.", resourceId);
                log.debug("Reserved connections {}", reservedConnections);
            }
        }

        return reservedSuccessfully.get();
    }

    @Override
    public boolean reserve(final int size) {
        String connectionId = ThreadManager.createUniqiueName();
        return reserve(connectionId, size);
    }


    @Override
    public boolean reserveOne() {
        String connectionId = ThreadManager.createUniqiueName();
        return reserve(connectionId, 1);
    }

    @Override
    public Jedis borrow(final String resourceId) {
        List<Jedis> returnList = new ArrayList<>();
        reservedConnections.compute(resourceId, (s, jedisList) -> {
            if (Objects.isNull(jedisList)) {
                throw new ConnectionManagerException("First you should reserve connection");
            }

            if (jedisList.isEmpty()) {
                throw new ConnectionManagerException("There is no any free connection. Try later!");
            }

            Jedis jedis = jedisList.remove(jedisList.size() - 1);
            returnList.add(jedis);

            return jedisList;
        });

        return returnList.get(0);
    }


    @Override
    public Jedis borrow() {
        String connectionId = ThreadManager.getName();
        return borrow(connectionId);
    }

    @Override
    public void returnBack(final String resourceId, final Jedis connection) {
        reservedConnections.compute(resourceId, (s, jedisList) -> {
            if (Objects.isNull(jedisList)) {
                throw new ConnectionManagerException("Invalid resource_id " + resourceId);
            }

            jedisList.add(connection);

            return jedisList;
        });
    }

    @Override
    public void returnBack(Jedis connection) {
        String connectionId = ThreadManager.getName();
        returnBack(connectionId, connection);
    }


    @Override
    public <E> E doWithConnection(String resourceId, Function<Jedis, E> operation) {
        Jedis jedis = borrow(resourceId);
        try {
            return operation.apply(jedis);
        } finally {
            returnBack(resourceId, jedis);
        }
    }

    @Override
    public <E> E doWithConnection(Function<Jedis, E> operation) {
        String connectionId = ThreadManager.getName();
        return doWithConnection(connectionId, operation);
    }


    @Override
    public void free(String resourceId) {
        reservedConnections.compute(resourceId, (s, jedisList) -> {
            if (Objects.isNull(jedisList)) {
                throw new ConnectionManagerException("Invalid resource_id " + resourceId);
            }

            if (!jedisList.isEmpty()) {
                jedisList.forEach(Jedis::close);
            }

            jedisList.clear();
            log.debug("Free connections for resource_id [{}] successfully", resourceId);
            return null;
        });
    }

    @Override
    public void free() {
        String connectionId = ThreadManager.getName();
        free(connectionId);
    }
}
