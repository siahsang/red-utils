package org.github.siahsang.redutils.lock;

import org.github.siahsang.redutils.common.RedUtilsConfig;
import org.github.siahsang.redutils.common.Scheduler;
import org.github.siahsang.redutils.exception.RefreshLockException;
import org.github.siahsang.redutils.replica.ReplicaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

public class JedisLockRefresher implements LockRefresher {
    private static final Logger log = LoggerFactory.getLogger(JedisLockRefresher.class);

    private final RedUtilsConfig redUtilsConfig;

    private final ReplicaManager replicaManager;

    private final Jedis jedis;

    private final Map<String, LockExecutionInfo> locksHolder = new ConcurrentHashMap<>();

    public JedisLockRefresher(RedUtilsConfig redUtilsConfig, ReplicaManager replicaManager, Jedis jedis) {
        this.redUtilsConfig = redUtilsConfig;
        this.replicaManager = replicaManager;
        this.jedis = jedis;
    }

    @Override
    public CompletableFuture<Void> start(final String lockName) {

        locksHolder.computeIfAbsent(lockName, lName -> {
            final int refreshPeriodMillis = redUtilsConfig.getLeaseTimeMillis();
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

            CompletableFuture<Void> resultCompletableFuture = Scheduler.scheduleAtFixRate(executor, () -> {
                try {
                    log.trace("Refreshing the lock [{}]", lockName);
                    jedis.pexpire(lockName, refreshPeriodMillis);
                    replicaManager.waitForResponse(redUtilsConfig.getReplicaCount(),
                            redUtilsConfig.getWaitingTimeForReplicasMillis(),
                            redUtilsConfig.getRetryCountForSyncingWithReplicas());
                } catch (Exception ex) {
                    String errMSG = String.format("Error in refreshing the lock '%s'", lockName);
                    throw new RefreshLockException(errMSG, ex);
                }
            }, refreshPeriodMillis / 3, refreshPeriodMillis / 3, TimeUnit.MILLISECONDS);

            return new LockExecutionInfo(executor, resultCompletableFuture);
        });

        return locksHolder.get(lockName).resultCompletableFuture;

    }

    @Override
    public void stop(final String lockName) {
        locksHolder.computeIfPresent(lockName, (lName, scheduledExecutorService) -> {
            scheduledExecutorService.scheduledExecutorService.shutdownNow();
            return null;
        });
    }

    private static class LockExecutionInfo {
        public final ScheduledExecutorService scheduledExecutorService;

        public final CompletableFuture<Void> resultCompletableFuture;

        public LockExecutionInfo(ScheduledExecutorService scheduledExecutorService,
                                 CompletableFuture<Void> resultCompletableFuture) {
            this.scheduledExecutorService = scheduledExecutorService;
            this.resultCompletableFuture = resultCompletableFuture;
        }
    }
}