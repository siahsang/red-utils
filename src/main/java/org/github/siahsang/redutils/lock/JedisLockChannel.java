package org.github.siahsang.redutils.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

public class JedisLockChannel implements LockChannel {
    private final Logger log = LoggerFactory.getLogger(JedisLockChannel.class);


    private final ConcurrentHashMap<String, ChannelInfo> lockNameChannelInfo = new ConcurrentHashMap<>();

    private final String unlockedMessagePattern;

    private final JedisPool connectionPool;

    public JedisLockChannel(JedisPool connectionPool, final String unlockedMessagePattern) {
        this.connectionPool = connectionPool;
        this.unlockedMessagePattern = unlockedMessagePattern;
    }

    @Override
    public void subscribe(final String lockName) {
        lockNameChannelInfo.compute(lockName, (s, channelInfo) -> {
            final long threadId = Thread.currentThread().getId();
            if (channelInfo == null) {
                Jedis jedis = connectionPool.getResource();
                channelInfo = new ChannelInfo(new LockChannelInfo(lockName, jedis, unlockedMessagePattern));
            }
            channelInfo.addSubscriber(threadId);

            return channelInfo;
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


        lockNameChannelInfo.get(lockName).lockChannelInfo.waitForGettingNotificationFromChannel(timeOutMillis);
    }

    @Override
    public void unSubscribe(final String lockName) {
        lockNameChannelInfo.compute(lockName, (lock, redisChannel) -> {
            final long threadId = Thread.currentThread().getId();

            if (redisChannel == null) {
                throw new IllegalArgumentException("There isn`t any channel with name " + lockName);
            }
            // if all subscriber removed, it means we do not need to preserve channel
            lockNameChannelInfo.get(lockName).removeSubscriber(threadId);
            if (lockNameChannelInfo.get(lockName).isSubscribersEmpty()) {
                lockNameChannelInfo.get(lockName).lockChannelInfo.shutdown();
                return null;
            }

            return redisChannel;
        });
    }


    private static class ChannelInfo {
        private final LockChannelInfo lockChannelInfo;

        private final Set<Long> subscribers = new HashSet<>();


        private ChannelInfo(LockChannelInfo lockChannelInfo) {
            this.lockChannelInfo = lockChannelInfo;
        }

        private void addSubscriber(Long id) {
            subscribers.add(id);
        }

        private void removeSubscriber(Long id) {
            subscribers.remove(id);
        }

        private boolean isSubscribersEmpty() {
            return subscribers.isEmpty();
        }
    }
}
