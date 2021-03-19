package org.github.siahsang.redutils.common.connection;

import org.github.siahsang.redutils.AbstractBaseTest;
import org.github.siahsang.redutils.common.RedUtilsConfig;
import org.github.siahsang.redutils.exception.BadRequestException;
import org.github.siahsang.redutils.exception.InsufficientResourceException;
import org.github.siahsang.test.redis.RedisAddress;
import org.github.siahsang.test.redis.RedisServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import redis.clients.jedis.Jedis;

/**
 * @author Javad Alimohammadi
 */

@Testcontainers
class JedisConnectionManagerTest extends AbstractBaseTest {

    private final static RedisAddress GENERAL_REDIS_ADDRESS = new RedisServer().startSingleInstance();

    private final static int INITIAL_CAPACITY = 10;

    private JedisConnectionManager jedisConnectionManager;

    @BeforeEach
    public void init() {
        RedUtilsConfig redUtilsConfig = new RedUtilsConfig.
                RedUtilsConfigBuilder()
                .hostAddress(GENERAL_REDIS_ADDRESS.masterHostAddress)
                .port(GENERAL_REDIS_ADDRESS.masterPort)
                .maxPoolSize(INITIAL_CAPACITY)
                .build();

        jedisConnectionManager = new JedisConnectionManager(redUtilsConfig);

    }

    @Test
    public void WHEN_get_connection_THEN_happy_path() throws Exception {
        //************************
        //          Given
        //************************

        //************************
        //        WHEN - THEN
        //************************
        boolean reserveFiveConnection = jedisConnectionManager.reserve(7);
        Assertions.assertTrue(reserveFiveConnection);


        Jedis jedisConnection1 = jedisConnectionManager.borrow();
        Jedis jedisConnection2 = jedisConnectionManager.borrow();
        Jedis jedisConnection3 = jedisConnectionManager.borrow();
        Jedis jedisConnection4 = jedisConnectionManager.borrow();
        Jedis jedisConnection5 = jedisConnectionManager.borrow();
        Jedis jedisConnection6 = jedisConnectionManager.borrow();
        Jedis jedisConnection7 = jedisConnectionManager.borrow();

        Assertions.assertEquals(3, jedisConnectionManager.remainingCapacity());


        jedisConnectionManager.returnBack(jedisConnection1);
        jedisConnectionManager.returnBack(jedisConnection2);
        jedisConnectionManager.returnBack(jedisConnection3);
        jedisConnectionManager.returnBack(jedisConnection4);
        jedisConnectionManager.returnBack(jedisConnection5);
        jedisConnectionManager.returnBack(jedisConnection6);
        jedisConnectionManager.returnBack(jedisConnection7);

        Assertions.assertEquals(INITIAL_CAPACITY, jedisConnectionManager.remainingCapacity());
    }


    @Test
    public void WHEN_get_more_connection_than_capacity_THEN_false_SHOULD_be_returned() throws Exception {
        //************************
        //          Given
        //************************

        //************************
        //          WHEN
        //************************
        boolean reservedSuccessfully = jedisConnectionManager.reserve(INITIAL_CAPACITY + 1);

        //************************
        //          THEN
        //************************
        Assertions.assertFalse(reservedSuccessfully);
        Assertions.assertEquals(INITIAL_CAPACITY, jedisConnectionManager.remainingCapacity());
    }


    @Test
    public void WHEN_brow_connection_more_than_reserved_THEN_exception_SHOULD_be_thrown() throws Exception {
        //************************
        //          Given
        //************************

        //************************
        //          WHEN
        //************************
        jedisConnectionManager.reserve(3);

        //************************
        //          THEN
        //************************
        jedisConnectionManager.borrow();
        jedisConnectionManager.borrow();
        jedisConnectionManager.borrow();

        Assertions.assertThrows(InsufficientResourceException.class, () -> {
            jedisConnectionManager.borrow();
        });

    }


    @Test
    public void WHEN_get_connection_with_invalid_name_THEN_exception_SHOULD_be_thrown() throws Exception {
        //************************
        //          Given
        //************************


        //************************
        //          WHEN
        //************************
        jedisConnectionManager.reserve(3);

        //************************
        //          THEN
        //************************
        Assertions.assertThrows(BadRequestException.class, () -> {
            jedisConnectionManager.borrow("invalid_resource_id");
        });


    }

    @Test
    public void WHEN_get_connection_with_multiple_thread_THEN_capacity_SHOULD_be_set_accordingly() throws Exception {
        //************************
        //          Given
        //************************

        //************************
        //          WHEN
        //************************
        Thread firstThread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        jedisConnectionManager.reserve(3);
                    }
                });

        Thread secondThread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        jedisConnectionManager.reserve(3);
                    }
                });

        Thread thirdThread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        jedisConnectionManager.reserve(3);
                    }
                });

        firstThread.start();
        secondThread.start();
        thirdThread.start();

        firstThread.join();
        secondThread.join();
        thirdThread.join();

        //************************
        //          THEN
        //************************

        Assertions.assertEquals(1, jedisConnectionManager.remainingCapacity());

    }

}