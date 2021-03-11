package org.github.siahsang.redutils;

import org.github.siahsang.redutils.common.OperationCallBack;
import org.github.siahsang.redutils.common.RedUtilsConfig;
import org.github.siahsang.redutils.common.ThreadManager;
import org.github.siahsang.redutils.common.connection.JedisConnectionManager;
import org.github.siahsang.redutils.common.redis.LuaScript;
import org.github.siahsang.redutils.common.redis.RedisResponse;
import org.github.siahsang.redutils.lock.JedisLockChannel;
import org.github.siahsang.redutils.lock.JedisLockRefresher;
import org.github.siahsang.redutils.lock.LockChannel;
import org.github.siahsang.redutils.lock.LockRefresher;
import org.github.siahsang.redutils.replica.JedisReplicaManager;
import org.github.siahsang.redutils.replica.ReplicaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
// todo : remove dependency to jedis
public class RedUtilsLockImpl implements RedUtilsLock {
    private static final Logger log = LoggerFactory.getLogger(RedUtilsLockImpl.class);

    private final ExecutorService operationExecutorService = Executors.newCachedThreadPool();

    private final LockChannel lockChannel;

    private final ReplicaManager replicaManager;

    private final RedUtilsConfig redUtilsConfig;

    private final JedisConnectionManager connectionManager;

    private final ThreadManager threadManager;

    public RedUtilsLockImpl() {
        this(RedUtilsConfig.DEFAULT_HOST_ADDRESS, RedUtilsConfig.DEFAULT_PORT, 0);
    }

    public RedUtilsLockImpl(final String hostAddress, final int port) {
        this(hostAddress, port, 0);
    }

    /**
     * Use with master-replica configuration
     *
     * @param replicaCount number of replica
     */
    public RedUtilsLockImpl(final String hostAddress, final int port, final int replicaCount) {
        this(new RedUtilsConfig
                .RedUtilsConfigBuilder()
                .hostAddress(hostAddress)
                .port(port)
                .replicaCount(replicaCount)
                .build()
        );
    }

    public RedUtilsLockImpl(RedUtilsConfig redUtilsConfig) {
        this.redUtilsConfig = redUtilsConfig;
        this.connectionManager = new JedisConnectionManager(redUtilsConfig);
        this.lockChannel = new JedisLockChannel(connectionManager, redUtilsConfig.getUnlockedMessagePattern());
        this.replicaManager = new JedisReplicaManager(connectionManager, redUtilsConfig.getReplicaCount(),
                redUtilsConfig.getRetryCountForSyncingWithReplicas(), redUtilsConfig.getWaitingTimeForReplicasMillis());
        this.threadManager = new ThreadManager();
    }

    @Override
    public boolean tryAcquire(final String lockName, final OperationCallBack operationCallBack) {

        if (!connectionManager.reserveOne()) {
            throw new RuntimeException("There is no any connection, please try again or change connection configs");
        }

        boolean getLockSuccessfully = getLock(lockName, redUtilsConfig.getLeaseTimeMillis());
        LockRefresher lockRefresher = null;
        if (getLockSuccessfully) {
            try {
                lockRefresher = new JedisLockRefresher(redUtilsConfig, replicaManager, connectionManager);
                CompletableFuture<Void> lockRefresherFuture = lockRefresher.start(lockName);
                CompletableFuture<Void> mainOperationFuture = CompletableFuture.runAsync(operationCallBack::doOperation,
                        operationExecutorService);

                lockRefresherFuture.exceptionally(throwable -> {
                    mainOperationFuture.completeExceptionally(throwable);
                    return null;
                });

                mainOperationFuture.join();

            } finally {
                lockRefresher.tryStop(lockName);
                tryReleaseLock(lockName);
                tryNotifyOtherClients(lockName);
                connectionManager.free();
            }

            return true;
        }

        return false;
    }

    @Override
    public void acquire(final String lockName, final OperationCallBack operationCallBack) {
        if (!connectionManager.reserve(2)) {
            throw new RuntimeException("There is no any connection, please try again or change connection configs");
        }

        boolean getLockSuccessfully = getLock(lockName, redUtilsConfig.getLeaseTimeMillis());

        if (!getLockSuccessfully) {
            try {
                lockChannel.subscribe(lockName);

                while (!getLockSuccessfully) {
                    final long ttl = getTTL(lockName);
                    if (ttl > 0) {
                        lockChannel.waitForNotification(lockName, ttl);
                    } else {
                        getLockSuccessfully = getLock(lockName, redUtilsConfig.getLeaseTimeMillis());
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted");
            } finally {
                lockChannel.unSubscribe(lockName);
            }
        }

        // At this point we have the lock
        LockRefresher lockRefresher = new JedisLockRefresher(redUtilsConfig, replicaManager, connectionManager);
        try {
            CompletableFuture<Void> lockRefresherStatus = lockRefresher.start(lockName);
            CompletableFuture<Void> mainOperationFuture = CompletableFuture.runAsync(operationCallBack::doOperation,
                    operationExecutorService);

            lockRefresherStatus.exceptionally(throwable -> {
                mainOperationFuture.completeExceptionally(throwable);
                return null;
            });

            mainOperationFuture.join();
        } finally {
            lockRefresher.tryStop(lockName);
            tryReleaseLock(lockName);
            tryNotifyOtherClients(lockName);
            connectionManager.free();
        }
    }


    private boolean getLock(final String lockName, final long expirationTimeMillis) {

        final String lockValue = threadManager.generateUniqueValue();

        try {
            Object response = connectionManager.doWithConnection(jedis -> {
                return jedis.eval(LuaScript.GET_LOCK, 1, lockName, lockValue, String.valueOf(expirationTimeMillis));
            });
            if (RedisResponse.isFailed(response)) {
                return false;
            }
            replicaManager.waitForResponse(lockName);
            return true;
        } catch (Exception exception) {
            releaseLock(lockName);
            throw exception;
        }

    }

    private void releaseLock(String lockName) {
        String lockValue = threadManager.generateUniqueValue();
        connectionManager.doWithConnection(jedis -> {
            return jedis.eval(LuaScript.RELEASE_LOCK, 1, lockName, lockValue);
        });

    }

    private void tryReleaseLock(String lockName) {
        try {
            releaseLock(lockName);
        } catch (Exception ex) {
            log.debug("Could not release lock [{}]", lockName);
        }
    }

    private long getTTL(final String lockName) {
        return connectionManager.doWithConnection(jedis -> jedis.pttl(lockName));
    }


    public void tryNotifyOtherClients(final String lockName) {
        try {
            connectionManager.doWithConnection(lockName, jedis -> {
                return jedis.publish(lockName, redUtilsConfig.getUnlockedMessagePattern());
            });
        } catch (Exception exception) {
            // nothing
            log.debug("Error in notify [{}] to other clients", lockName, exception);
        }
    }

}
