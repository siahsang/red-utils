package org.github.siahsang.redutils.common.connection;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

public abstract class ConnectionPoolFactory {
    private ConnectionPoolFactory() {
    }

    public static GenericObjectPoolConfig<Jedis> makePool(final int maxSize) {
        GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(60000);
        poolConfig.setTimeBetweenEvictionRunsMillis(30000);
        poolConfig.setNumTestsPerEvictionRun(-1);

        poolConfig.setMaxTotal(maxSize);

        return poolConfig;
    }


}
