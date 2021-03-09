package org.github.siahsang.redutils;

import org.github.siahsang.redutils.exception.RefreshLockException;
import org.github.siahsang.redutils.replica.ReplicaManager;
import org.github.siahsang.redutils.common.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

public class LockRefresher {
    private static final Logger log = LoggerFactory.getLogger(LockRefresher.class);

    private final long refreshPeriodMillis;

    private final ReplicaManager replicaManager;

    private final Map<String, LockExecutionInfo> locksHolder = new ConcurrentHashMap<>();

    public LockRefresher(final long refreshPeriodMillis, final ReplicaManager replicaManager) {
        this.replicaManager = replicaManager;
        this.refreshPeriodMillis = refreshPeriodMillis;
    }


    public CompletableFuture<Void> start(final Jedis jedis, final String lockName) {

        locksHolder.computeIfAbsent(lockName, lName -> {
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

            CompletableFuture<Void> resultCompletableFuture = Scheduler.scheduleAtFixRate(executor, () -> {
                try {
                    log.trace("Refreshing the lock [{}]", lockName);
                    jedis.pexpire(lockName, refreshPeriodMillis);
                    replicaManager.waitForReplicaResponse(jedis);
                } catch (Exception ex) {
                    String errMSG = String.format("Error in refreshing the lock '%s'", lockName);
                    throw new RefreshLockException(errMSG, ex);
                }
            }, refreshPeriodMillis / 3, refreshPeriodMillis / 3, TimeUnit.MILLISECONDS);

            return new LockExecutionInfo(executor, resultCompletableFuture);
        });

        return locksHolder.get(lockName).resultCompletableFuture;

    }

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