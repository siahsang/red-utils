package org.github.siahsang.redutils.channel;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class hold information about subscribers and semaphore status for the channel
 *
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
public class RedisChannel {

    private final AtomicBoolean hasNotification = new AtomicBoolean(false);

    private final Semaphore notificationResource = new Semaphore(0);

    private final ChannelListener channelListener = new ChannelListener(this);

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final Jedis jedis;

    private final String unlockedMessagePattern;

    public RedisChannel(final String channelName, final Jedis jedis, final String unlockedMessagePattern) {
        this.jedis = jedis;
        this.unlockedMessagePattern = unlockedMessagePattern;

        executorService.submit(() -> {
            jedis.subscribe(channelListener, channelName);
        });
    }

    /**
     * Wait calling thread for getting notification from channel.
     *
     * @param timeOutMillis maximum amount of time for waiting to get
     * @throws InterruptedException
     */
    public void waitForGettingNotificationFromChannel(final long timeOutMillis) throws InterruptedException {

        boolean acquireNotificationRecourse = notificationResource.tryAcquire(timeOutMillis, TimeUnit.MILLISECONDS);

        // if true, this means we got a new message from channel and we consumed it
        if (acquireNotificationRecourse) {
            hasNotification.set(false);
        }

    }

    public void sendNotification() {
        if (hasNotification.compareAndSet(false, true)) {
            notificationResource.release();
        }
    }

    public void shutdown() {
        channelListener.unsubscribe();
        executorService.shutdownNow();
        jedis.close();
    }

    private static class ChannelListener extends JedisPubSub {

        private final RedisChannel redisChannel;


        private ChannelListener(RedisChannel redisChannel) {
            this.redisChannel = redisChannel;
        }

        @Override
        public void onMessage(String channel, String message) {
            if (message.startsWith(redisChannel.unlockedMessagePattern)) {
                redisChannel.sendNotification();
            }
        }
    }
}
