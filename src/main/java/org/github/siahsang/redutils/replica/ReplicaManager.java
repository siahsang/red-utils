package org.github.siahsang.redutils.replica;

import redis.clients.jedis.Jedis;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
public interface ReplicaManager {
    void waitForReplicaResponse(Jedis jedis);
}
