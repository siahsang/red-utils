package org.github.siahsang.redutils.lock;

import org.github.siahsang.redutils.common.RedUtilsConfig;
import org.github.siahsang.redutils.common.Scheduler;
import org.github.siahsang.redutils.common.connection.ConnectionManager;
import org.github.siahsang.redutils.exception.RefreshLockException;
import org.github.siahsang.redutils.replica.ReplicaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

public class JedisLockRefresher implements LockRefresher {
    private static final Logger log = LoggerFactory.getLogger(JedisLockRefresher.class);

    private final RedUtilsConfig redUtilsConfig;

    private final ReplicaManager replicaManager;

    private final ConnectionManager<Jedis> jedisConnectionManager;

    private final ScheduledExecutorService executor;

    public JedisLockRefresher(RedUtilsConfig redUtilsConfig, ReplicaManager replicaManager,
                              ConnectionManager<Jedis> jedisConnectionManager) {
        this.redUtilsConfig = redUtilsConfig;
        this.replicaManager = replicaManager;
        this.jedisConnectionManager = jedisConnectionManager;
        this.executor = Executors.newScheduledThreadPool(1);
    }

    @Override
    public CompletableFuture<Void> start(final String lockName) {
        final int refreshPeriodMillis = redUtilsConfig.getLeaseTimeMillis();

        return Scheduler.scheduleAtFixRate(executor, () -> {
            try {
                log.trace("Refreshing the lock [{}]", lockName);
                jedisConnectionManager.doWithConnection(jedis -> {
                    return jedis.pexpire(lockName, refreshPeriodMillis);
                });
                replicaManager.waitForResponse();
            } catch (Exception ex) {
                String errMSG = String.format("Error in refreshing the lock '%s'", lockName);
                throw new RefreshLockException(errMSG, ex);
            }
        }, refreshPeriodMillis / 3, refreshPeriodMillis / 3, TimeUnit.MILLISECONDS);
    }

    @Override
    public void tryStop(final String lockName) {
        try {
            executor.shutdownNow();
        } catch (Exception exception) {
            log.debug("Error in stopping Refresher", exception);
        }
    }

}