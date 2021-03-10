package org.github.siahsang.redutils.common.resource;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.github.siahsang.redutils.common.RedUtilsConfig;
import org.github.siahsang.redutils.common.ResourcePoolFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
public class JedisConnectionManager implements ConnectionManager<Jedis> {
    private final AtomicInteger maximumAllowedConnection = new AtomicInteger(0);

    private final Map<String, List<Jedis>> reservedConnections = new ConcurrentHashMap<>();


    private final JedisPool channelConnectionPool;

    public JedisConnectionManager(RedUtilsConfig redUtilsConfig) {
        this.maximumAllowedConnection.set(redUtilsConfig.getLockMaxPoolSize());

        GenericObjectPoolConfig<Jedis> lockPoolConfig = ResourcePoolFactory.makePool(maximumAllowedConnection.get());

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
    public boolean reserveOne(final String id) {
        return reserve(id, 1);
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
}
