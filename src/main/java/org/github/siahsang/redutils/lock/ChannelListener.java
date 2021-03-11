package org.github.siahsang.redutils.lock;

import redis.clients.jedis.JedisPubSub;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
public class ChannelListener extends JedisPubSub {
    private final LockChannelInfo lockChannelInfo;

    private final String unlockedMessagePattern;

    public ChannelListener(LockChannelInfo lockChannelInfo, String unlockedMessagePattern) {
        this.lockChannelInfo = lockChannelInfo;
        this.unlockedMessagePattern = unlockedMessagePattern;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (message.startsWith(unlockedMessagePattern)) {
            lockChannelInfo.notifyMessage();
        }
    }
}
