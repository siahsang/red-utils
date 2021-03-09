package org.github.siahsang.redutils.lock;

import redis.clients.jedis.Jedis;

import java.util.concurrent.CompletableFuture;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
public interface LockRefresher {
    CompletableFuture<Void> start(Jedis jedis, String lockName);

    void stop(final String lockName);
}
