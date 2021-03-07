package org.github.siahsang.redutils;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.github.siahsang.redutils.channel.LockChannel;
import org.github.siahsang.redutils.pool.ResourcePoolManager;
import org.github.siahsang.redutils.replica.ReplicaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

public class RedUtilsLockImpl implements RedUtilsLock {
    private static final Logger log = LoggerFactory.getLogger(RedUtilsLockImpl.class);

    private final ExecutorService operationExecutorService = Executors.newCachedThreadPool();

    // TODO: 2/26/21 make this properties configurable.
    public static final int LEASE_TIME_MILLIS = 30_000;

    private final String uuid = UUID.randomUUID().toString();

    private final JedisPool lockConnectionPool;

    private final LockRefresher lockRefresher;

    private final LockChannel lockChannel;

    private final ReplicaManager replicaManager;

    private RedUtilsConfig redUtilsConfig;

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
        GenericObjectPoolConfig<Jedis> lockPoolConfig = ResourcePoolManager.makePool(redUtilsConfig.getLockMaxPoolSize());
        GenericObjectPoolConfig<Jedis> channelPoolConfig = ResourcePoolManager.makePool(redUtilsConfig.getChannelMaxPoolSize());
        JedisPool channelConnectionPool = new JedisPool(
                channelPoolConfig,
                redUtilsConfig.getHostAddress(),
                redUtilsConfig.getPort(),
                redUtilsConfig.getReadTimeOutMillis()
        );

        this.lockConnectionPool = new JedisPool(
                lockPoolConfig,
                redUtilsConfig.getHostAddress(),
                redUtilsConfig.getPort(),
                redUtilsConfig.getReadTimeOutMillis()
        );

        this.lockChannel = new LockChannel(channelConnectionPool, redUtilsConfig.getRedUtilsUnLockedMessagePattern());

        this.replicaManager = new ReplicaManager(
                redUtilsConfig.getReplicaCount(),
                redUtilsConfig.getWaitingTimeForReplicasMillis(),
                redUtilsConfig.getRetryCountForSyncingWithReplicas()
        );
        this.lockRefresher = new LockRefresher(redUtilsConfig.getLeaseTimeMillis(), replicaManager);

    }


    @Override
    public boolean tryAcquire(final String lockName, final OperationCallBack operationCallBack) {

        try (Jedis jedis = lockConnectionPool.getResource()) {

            boolean getLockSuccessfully = getLock(jedis, lockName, LEASE_TIME_MILLIS);

            if (getLockSuccessfully) {
                try {
                    CompletableFuture<Void> lockRefresherFuture = lockRefresher.start(jedis, lockName);
                    CompletableFuture<Void> mainOperationFuture = CompletableFuture.runAsync(operationCallBack::doOperation,
                            operationExecutorService);

                    lockRefresherFuture.exceptionally(throwable -> {
                        mainOperationFuture.completeExceptionally(throwable);
                        return null;
                    });

                    mainOperationFuture.join();

                } finally {
                    lockRefresher.stop(lockName);
                    tryReleaseLock(jedis, lockName);
                    tryNotifyOtherClients(jedis, lockName);
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public void acquire(final String lockName, final OperationCallBack operationCallBack) {
        try (Jedis jedis = lockConnectionPool.getResource()) {
            boolean getLockSuccessfully = getLock(jedis, lockName, LEASE_TIME_MILLIS);

            if (!getLockSuccessfully) {
                try {
                    lockChannel.subscribe(lockName);

                    while (!getLockSuccessfully) {
                        final long ttl = getTTL(jedis, lockName);
                        if (ttl > 0) {
                            lockChannel.waitForNotification(lockName, ttl);
                        } else {
                            getLockSuccessfully = getLock(jedis, lockName, LEASE_TIME_MILLIS);
                        }
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted");
                } finally {
                    lockChannel.unSubscribe(lockName);
                }
            }

            try {
                CompletableFuture<Void> lockRefresherStatus = lockRefresher.start(jedis, lockName);
                CompletableFuture<Void> mainOperationFuture = CompletableFuture.runAsync(operationCallBack::doOperation,
                        operationExecutorService);

                lockRefresherStatus.exceptionally(throwable -> {
                    mainOperationFuture.completeExceptionally(throwable);
                    return null;
                });

                mainOperationFuture.join();
            } finally {
                lockRefresher.stop(lockName);
                tryReleaseLock(jedis, lockName);
                tryNotifyOtherClients(jedis, lockName);
            }
        }
    }


    private boolean getLock(final Jedis jedis, final String lockName, final long expirationTimeMillis) {

        final String lockValue = createLockValue();

        try {
            Object response = jedis.eval(LuaScript.GET_LOCK, 1, lockName, lockValue, String.valueOf(expirationTimeMillis));
            if (RedisResponse.isNotOK(response)) {
                return false;
            }
            replicaManager.waitForReplicaResponse(jedis);
            return true;
        } catch (Exception exception) {
            releaseLock(jedis, lockName);
            throw exception;
        }

    }

    private void releaseLock(Jedis jedis, String lockName) {
        String lockValue = createLockValue();
        jedis.eval(LuaScript.RELEASE_LOCK, 1, lockName, lockValue);
    }

    private void tryReleaseLock(Jedis jedis, String lockName) {
        try {
            releaseLock(jedis, lockName);
        } catch (Exception ex) {
            log.debug("Could not release lock [{}]", lockName);
        }
    }

    private long getTTL(Jedis jedis, final String lockName) {
        return jedis.pttl(lockName);
    }


    private String createLockValue() {
        long threadId = Thread.currentThread().getId();
        return threadId + ":" + uuid;
    }

    public void tryNotifyOtherClients(final Jedis jedis, final String lockName) {
        try {
            jedis.publish(lockName, redUtilsConfig.getRedUtilsUnLockedMessagePattern());
        } catch (Exception exception) {
            // nothing
            log.error("Error in notify [{}] to other clients", lockName, exception);
        }
    }

}