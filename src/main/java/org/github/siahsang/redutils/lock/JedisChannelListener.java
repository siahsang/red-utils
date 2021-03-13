package org.github.siahsang.redutils.lock;

import org.github.siahsang.redutils.common.connection.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
public class JedisChannelListener extends ChannelListener {
    private final Logger log = LoggerFactory.getLogger(JedisLockChannel.class);

    private final String unlockedMessagePattern;

    private JedisPubSub jedisPubSub;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final String channelName;

    private final ConnectionManager<Jedis> jedisConnectionManager;

    private Jedis jedis;

    public JedisChannelListener(String unlockedMessagePattern, String channelName, ConnectionManager<Jedis> jedisConnectionManager) {
        this.unlockedMessagePattern = unlockedMessagePattern;
        this.channelName = channelName;
        this.jedisConnectionManager = jedisConnectionManager;

    }

    @Override
    public void shutdown() {
        try {
            jedisPubSub.unsubscribe();
        } catch (Exception exception) {
            log.debug("Error in unsubscribing channel " + channelName);
        }

        try {
            executorService.shutdownNow();
        } catch (Exception ex) {
            log.debug("Error in shut-down channel " + channelName);
        }

        jedisConnectionManager.returnBack(jedis);
    }

    @Override
    public void startListening() {
        this.jedisPubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (message.startsWith(unlockedMessagePattern)) {
                    onGettingNewMessage();
                }
            }
        };

        jedis = jedisConnectionManager.borrow();
        executorService.submit(() -> {
            jedis.subscribe(jedisPubSub, channelName);
        });
    }
}
