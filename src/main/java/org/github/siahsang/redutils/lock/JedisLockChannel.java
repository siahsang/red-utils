package org.github.siahsang.redutils.lock;

import org.github.siahsang.redutils.common.connection.ConnectionManager;
import org.github.siahsang.redutils.common.connection.JedisConnectionManager;
import redis.clients.jedis.Jedis;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

public class JedisLockChannel implements LockChannel {

    private final ConcurrentHashMap<String, ChannelListener> lockNameChannelInfo = new ConcurrentHashMap<>();

    private final String unlockedMessagePattern;

    private final ConnectionManager<Jedis> jedisConnectionManager;

    public JedisLockChannel(JedisConnectionManager jedisConnectionManager, final String unlockedMessagePattern) {
        this.jedisConnectionManager = jedisConnectionManager;
        this.unlockedMessagePattern = unlockedMessagePattern;
    }

    @Override
    public void subscribe(final String lockName) {
        lockNameChannelInfo.compute(lockName, (s, channelListener) -> {
            final long threadId = Thread.currentThread().getId();
            if (channelListener == null) {
                channelListener = new JedisChannelListener(unlockedMessagePattern, lockName, jedisConnectionManager);
                channelListener.startListening();
            }

            channelListener.addSubscriber(threadId);
            return channelListener;
        });
    }

    @Override
    public void waitForNotification(final String lockName, final long timeOutMillis) throws InterruptedException {
        lockNameChannelInfo.compute(lockName, (lname, redisChannel) -> {
            if (redisChannel == null) {
                throw new IllegalArgumentException("There isn`t any channel with name " + lockName);
            }
            return redisChannel;
        });

        lockNameChannelInfo.get(lockName).waitForGettingNotificationFromChannel(timeOutMillis);
    }

    @Override
    public void unSubscribe(final String lockName) {
        lockNameChannelInfo.compute(lockName, (lock, redisChannel) -> {
            final long threadId = Thread.currentThread().getId();

            if (redisChannel == null) {
                throw new IllegalArgumentException("There isn`t any channel with name " + lockName);
            }
            // if all subscriber removed, it means we do not need to preserve channel
            redisChannel.removeSubscriber(threadId);
            if (redisChannel.isSubscribersEmpty()) {
                redisChannel.shutdown();
                return null;
            }

            return redisChannel;
        });
    }

}
