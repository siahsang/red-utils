package org.github.siahsang.redutils;

import org.awaitility.Awaitility;
import org.github.siahsang.redutils.common.RedUtilsConfig;
import org.github.siahsang.redutils.exception.RefreshLockException;
import org.github.siahsang.redutils.exception.ReplicaIsDownException;
import org.github.siahsang.test.redis.RedisAddress;
import org.github.siahsang.test.redis.RedisServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

@Testcontainers
class RedUtilsLockImplTest extends AbstractBaseTest {

    private final static RedisAddress GENERAL_REDIS_ADDRESS = new RedisServer().startSingleInstance();

    private final static Jedis JEDIS = new Jedis(GENERAL_REDIS_ADDRESS.masterHostAddress, GENERAL_REDIS_ADDRESS.masterPort);

    @AfterEach
    void afterEach() {
        JEDIS.flushAll();
    }

    @Test
    void test_tryAcquire_WHEN_happy_path() throws Exception {
        //************************
        //          Given
        //************************
        RedUtilsLockImpl redUtilsLock = new RedUtilsLockImpl(GENERAL_REDIS_ADDRESS.masterHostAddress, GENERAL_REDIS_ADDRESS.masterPort);

        //************************
        //          WHEN
        //************************
        AtomicBoolean firstTryToGetLock = new AtomicBoolean(false);
        boolean gotLockSuccessfully = redUtilsLock.tryAcquire("lock", () -> {
            firstTryToGetLock.set(true);
        });


        //************************
        //          THEN
        //************************
        Assertions.assertTrue(firstTryToGetLock.get());
        Assertions.assertTrue(gotLockSuccessfully);
        Assertions.assertNull(getKey("lock"));
    }

    @Test
    void test_tryAcquire_WHEN_redis_stop_after_getting_lock_THEN_we_SHOULD_NOT_able_to_get_new_lock() throws Exception {
        //************************
        //          Given
        //************************
        RedisServer redisServer = new RedisServer();
        RedisAddress redisAddress = redisServer.startSingleInstance();

        RedUtilsLockImpl redUtilsLock = new RedUtilsLockImpl(redisAddress.masterHostAddress, redisAddress.masterPort);

        AtomicBoolean firstThreadTryToGetLock = new AtomicBoolean(false);

        //************************
        //          WHEN
        //************************
        Thread firstThread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        redUtilsLock.tryAcquire("lock", () -> {
                            firstThreadTryToGetLock.set(true);
                            sleepSeconds(5);
                        });
                    }
                });

        firstThread.start();
        Awaitility.await("check first thread can get the lock").untilTrue(firstThreadTryToGetLock);

        redisServer.shutDown();

        //************************
        //          THEN
        //************************
        Assertions.assertThrows(JedisConnectionException.class, () -> {
            redUtilsLock.tryAcquire("lock", () -> {
                sleepSeconds(5);
            });
        });
    }

    @Test
    void test_tryAcquire_WHEN_redis_become_unavailable_after_getting_lock_THEN_we_SHOULD_get_exception() throws Exception {
        //************************
        //          Given
        //************************
        RedisServer redisServer = new RedisServer();
        RedisAddress redisAddress = redisServer.startSingleInstance();
        RedUtilsConfig redUtilsConfig = new RedUtilsConfig.RedUtilsConfigBuilder()
                .hostAddress(redisAddress.masterHostAddress)
                .port(redisAddress.masterPort)
                .leaseTimeMillis(10_000)
                .build();

        RedUtilsLockImpl redUtilsLock = new RedUtilsLockImpl(redUtilsConfig);
        AtomicBoolean firstThreadTryToGetLock = new AtomicBoolean(false);
        AtomicReference<Throwable> raisedException = new AtomicReference<>();

        //************************
        //          WHEN
        //************************
        CompletableFuture<Void> runningFirstThreadFuture = CompletableFuture.runAsync(() -> {
            redUtilsLock.tryAcquire("lock1", () -> {
                firstThreadTryToGetLock.set(true);
                sleepSeconds(60);
            });
        });

        runningFirstThreadFuture.exceptionally(throwable -> {
            raisedException.set(throwable.getCause());
            return null;
        });

        Awaitility.await("check first thread can get the lock").forever().untilTrue(firstThreadTryToGetLock);

        redisServer.pauseMaster(60);

        //************************
        //          THEN
        //************************
        try {
            runningFirstThreadFuture.join();
        } catch (Exception exception) {
            Awaitility.await("check raised exception is set").until(() -> {
                return raisedException.get() != null;
            });
        }

        Assertions.assertTrue(runningFirstThreadFuture.isCompletedExceptionally());
        Assertions.assertTrue(raisedException.get() instanceof RefreshLockException);
    }

    @Test
    void test_tryAcquire_WHEN_two_threads_want_to_get_lock_THEN_last_one_SHOULD_NOT_be_able_to_get_lock() throws Exception {
        //************************
        //          Given
        //************************
        RedUtilsLockImpl redUtilsLock = new RedUtilsLockImpl(GENERAL_REDIS_ADDRESS.masterHostAddress, GENERAL_REDIS_ADDRESS.masterPort);

        AtomicBoolean firstThreadTryToGetLock = new AtomicBoolean(false);
        AtomicBoolean secondThreadTryToGetLock = new AtomicBoolean(false);

        AtomicBoolean firstThreadGotLockSuccessfully = new AtomicBoolean(false);
        AtomicBoolean secondThreadGotLockSuccessfully = new AtomicBoolean(false);

        //************************
        //          WHEN
        //************************

        Thread firstThread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        boolean isSuccess = redUtilsLock.tryAcquire("lock", () -> {
                            firstThreadTryToGetLock.set(true);
                            sleepSeconds(3);
                        });

                        firstThreadGotLockSuccessfully.set(isSuccess);

                    }
                });

        firstThread.start();

        Awaitility.await("check first thread can get the lock").untilTrue(firstThreadTryToGetLock);

        Thread secondThread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        boolean isSuccess = redUtilsLock.tryAcquire("lock", () -> {
                            secondThreadTryToGetLock.set(true);
                        });

                        secondThreadGotLockSuccessfully.set(isSuccess);
                    }
                });

        secondThread.start();

        firstThread.join();
        secondThread.join();

        //************************
        //          THEN
        //************************
        Assertions.assertTrue(firstThreadTryToGetLock.get());
        Assertions.assertTrue(firstThreadGotLockSuccessfully.get());
        Assertions.assertFalse(secondThreadTryToGetLock.get());
        Assertions.assertFalse(secondThreadGotLockSuccessfully.get());
    }

    @Test
    void test_acquire_WHEN_two_threads_want_to_get_lock_THEN_last_one_SHOULD_NOT_be_able_to_get_lock() throws Exception {
        //************************
        //          Given
        //************************
        RedUtilsLockImpl redUtilsLock = new RedUtilsLockImpl(GENERAL_REDIS_ADDRESS.masterHostAddress, GENERAL_REDIS_ADDRESS.masterPort);

        AtomicBoolean firstTryToGetLock = new AtomicBoolean(false);
        AtomicBoolean secondTryToGetLock = new AtomicBoolean(false);


        //************************
        //          WHEN
        //************************
        Thread firstThread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        redUtilsLock.acquire("lock1", () -> {
                            firstTryToGetLock.set(true);
                            sleepSeconds(12);
                        });
                    }
                });

        firstThread.start();

        Awaitility.await("check first thread can get the lock").untilTrue(firstTryToGetLock);


        Thread secondThread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        redUtilsLock.acquire("lock1", () -> {
                            secondTryToGetLock.set(true);
                        });
                    }
                });

        secondThread.start();

        Awaitility.await("second thread should not get lock").pollDelay(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(3))
                .untilFalse(secondTryToGetLock);


        //************************
        //          THEN
        //************************
        Assertions.assertTrue(firstTryToGetLock.get());
        Assertions.assertFalse(secondTryToGetLock.get());

    }

    @Disabled("Disabled until implement reentrancy")
    @Test()
    void test_get_lock_WHEN_reentrancy_in_the_same_thread_we_SHOULD_able_to_proceed() throws Exception {
        //************************
        //          Given
        //************************
        RedisServer redisServer = new RedisServer();
        RedisAddress redisAddress = redisServer.startSingleInstance();
        RedUtilsLockImpl redUtilsLock = new RedUtilsLockImpl(redisAddress.masterHostAddress, redisAddress.masterPort);

        AtomicBoolean firstLock = new AtomicBoolean(false);
        AtomicBoolean secondLock = new AtomicBoolean(false);

        //************************
        //          WHEN
        //************************
        Thread firstThread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        redUtilsLock.acquire("lock1", () -> {
                            firstLock.set(true);

                            redUtilsLock.acquire("lock1", () -> {
                                secondLock.set(true);
                            });
                        });
                    }
                });

        firstThread.start();


        Awaitility.await("check thread can get the first lock").atMost(Duration.ofMinutes(10)).untilTrue(firstLock);
        Awaitility.await("check thread can get the second lock").atMost(Duration.ofMinutes(10)).untilTrue(secondLock);


        //************************
        //          THEN
        //************************
        Assertions.assertTrue(firstLock.get());
        Assertions.assertFalse(secondLock.get());
    }

    @Test
    void test_acquire_WHEN_happy_path() throws Exception {
        //************************
        //          Given
        //************************
        RedUtilsLockImpl redUtilsLock = new RedUtilsLockImpl(GENERAL_REDIS_ADDRESS.masterHostAddress, GENERAL_REDIS_ADDRESS.masterPort);
        AtomicBoolean firstTryToGetLock = new AtomicBoolean(false);


        //************************
        //          WHEN
        //************************
        redUtilsLock.acquire("lock1", () -> {
            sleepSeconds(2);
            firstTryToGetLock.set(true);
        });


        //************************
        //          THEN
        //************************
        Assertions.assertTrue(firstTryToGetLock.get());
        Assertions.assertNull(getKey("lock1"));

    }

    @Test
    void test_acquire_get_lock_multiple_time() throws Exception {
        //************************
        //          Given
        //************************
        RedUtilsLockImpl redUtilsLock = new RedUtilsLockImpl(GENERAL_REDIS_ADDRESS.masterHostAddress, GENERAL_REDIS_ADDRESS.masterPort);
        AtomicBoolean tryToGetLock = new AtomicBoolean(false);

        //************************
        //          WHEN
        //************************
        redUtilsLock.acquire("lock1", () -> {
            sleepSeconds(1);
            tryToGetLock.set(true);
        });

        tryToGetLock.set(false);

        redUtilsLock.acquire("lock1", () -> {
            sleepSeconds(1);
            tryToGetLock.set(true);
        });

        //************************
        //          THEN
        //************************
        Assertions.assertTrue(tryToGetLock.get());
        Assertions.assertNull(getKey("lock1"));

    }

    @Test
    void test_acquire_WHEN_single_client_AND_multiple_threads_process_the_same_resource_THEN_we_SHOULD_get_correct_result() throws Exception {
        //************************
        //          Given
        //************************
        final int threadCount = 30;
        final int expectedResourceValue = threadCount;
        final AtomicInteger sharedResource = new AtomicInteger(0);
        RedUtilsLockImpl redUtilsLock = new RedUtilsLockImpl(GENERAL_REDIS_ADDRESS.masterHostAddress, GENERAL_REDIS_ADDRESS.masterPort);
        ExecutorService executorService = Executors.newCachedThreadPool();

        //************************
        //          WHEN
        //************************
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                redUtilsLock.acquire("lock1", () -> {
                    int resValue = sharedResource.get();
                    resValue = resValue + 1;
                    sharedResource.set(resValue);
                });
            });
        }
        executorService.shutdown();
        boolean allThreadExecutionFinished = executorService.awaitTermination(1, TimeUnit.MINUTES);

        //************************
        //          THEN
        //************************

        Assertions.assertTrue(allThreadExecutionFinished);
        Assertions.assertEquals(expectedResourceValue, sharedResource.get());
    }

    @Test
    void test_acquire_WHEN_multiple_client_AND_multiple_threads_process_the_same_resource_THEN_we_SHOULD_get_correct_result() throws Exception {
        //************************
        //          Given
        //************************
        final int threadCountPerClient = 10;
        final int clientCount = 5;
        final int expectedResourceValue = threadCountPerClient * clientCount;
        final AtomicInteger sharedResource = new AtomicInteger(0);
        final List<RedUtilsLock> clients = new ArrayList<>();

        for (int i = 0; i < clientCount; i++) {
            RedUtilsLockImpl redUtilsLock = new RedUtilsLockImpl(GENERAL_REDIS_ADDRESS.masterHostAddress, GENERAL_REDIS_ADDRESS.masterPort);
            clients.add(redUtilsLock);
        }

        ExecutorService executorService = Executors.newCachedThreadPool();

        //************************
        //          WHEN
        //************************
        for (int i = 0; i < clientCount; i++) {
            for (int j = 0; j < threadCountPerClient; j++) {
                final int clientId = i;
                executorService.submit(() -> {
                    clients.get(clientId).acquire("lock1", () -> {
                        int resValue = sharedResource.get();
                        resValue = resValue + 1;
                        sharedResource.set(resValue);
                    });
                });
            }
        }

        executorService.shutdown();
        boolean allThreadExecutionFinished = executorService.awaitTermination(1, TimeUnit.MINUTES);

        //************************
        //          THEN
        //************************
        Assertions.assertTrue(allThreadExecutionFinished);
        Assertions.assertEquals(expectedResourceValue, sharedResource.get());

    }

    @Test
    void test_acquire_WHEN_one_of_replicas_is_unavailable_THEN_we_SHOULD_get_exception() throws Exception {
        //************************
        //          Given
        //************************
        final int replicaCount = 3;
        RedisServer redisServer = new RedisServer();
        RedisAddress redisAddress = redisServer.startMasterReplicas(replicaCount);
        RedUtilsConfig redUtilsConfig = new RedUtilsConfig.RedUtilsConfigBuilder()
                .hostAddress(redisAddress.masterHostAddress)
                .port(redisAddress.masterPort)
                .replicaCount(replicaCount)
                .leaseTimeMillis(10_000)
                .build();

        RedUtilsLockImpl redUtilsLock = new RedUtilsLockImpl(redUtilsConfig);


        //************************
        //          WHEN
        //************************
        redisServer.shutdownReplica(1);
        //************************
        //          THEN
        //************************
        Assertions.assertThrows(ReplicaIsDownException.class, () -> {
            redUtilsLock.acquire("lock1", () -> {
                sleepSeconds(10);
            });
        });


    }

    @Test
    void test_acquire_WHEN_one_of_replicas_become_unavailable_after_getting_lock_THEN_we_SHOULD_get_exception() throws Exception {
        //************************
        //          Given
        //************************
        final int replicaCount = 3;
        RedisServer redisServer = new RedisServer();
        RedisAddress redisAddress = redisServer.startMasterReplicas(replicaCount);
        RedUtilsConfig redUtilsConfig = new RedUtilsConfig.RedUtilsConfigBuilder()
                .hostAddress(redisAddress.masterHostAddress)
                .port(redisAddress.masterPort)
                .replicaCount(replicaCount)
                .leaseTimeMillis(10_000)
                .build();

        RedUtilsLockImpl redUtilsLock = new RedUtilsLockImpl(redUtilsConfig);

        AtomicBoolean firstThreadTryToGetLock = new AtomicBoolean(false);
        AtomicReference<Throwable> raisedException = new AtomicReference<>();

        //************************
        //          WHEN
        //************************
        CompletableFuture<Void> runningFirstThreadFuture = CompletableFuture.runAsync(() -> {
            redUtilsLock.acquire("lock1", () -> {
                firstThreadTryToGetLock.set(true);
                sleepSeconds(60);
            });
        });

        runningFirstThreadFuture.exceptionally(throwable -> {
            raisedException.set(throwable.getCause());
            return null;
        });

        Awaitility.await("check first thread can get the lock").untilTrue(firstThreadTryToGetLock);

        redisServer.pauseReplica(1, 60);


        //************************
        //          THEN
        //************************
        try {
            runningFirstThreadFuture.join();
        } catch (Exception exception) {
            Awaitility.await("check raisedException is set").until(() -> {
                return raisedException.get() != null;
            });
        }

        Assertions.assertTrue(runningFirstThreadFuture.isCompletedExceptionally());
        Assertions.assertTrue(raisedException.get() instanceof RefreshLockException);
    }


    private String getKey(String key) {
        return JEDIS.get(key);
    }

}