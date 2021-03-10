package org.github.siahsang.redutils.replica;

import org.github.siahsang.redutils.exception.ReplicaIsDownException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

public class JedisReplicaManager implements ReplicaManager {
    private static final Logger log = LoggerFactory.getLogger(JedisReplicaManager.class);

    private final Jedis jedis;

    public JedisReplicaManager(Jedis jedis) {
        this.jedis = jedis;
    }

    @Override
    public void waitForResponse(final int replicaCount, final int waitingTimeMillis, final int retryCount) {
        if (replicaCount > 0) {
            int retry = 1;
            long replicaResponseCount = jedis.waitReplicas(replicaCount, waitingTimeMillis);

            while (replicaResponseCount != replicaCount && retry <= retryCount) {
                log.warn("Expected number of replica(s) is [{}] but available number of replica(s) is [{}], trying again({})",
                        replicaCount, replicaResponseCount, retry);
                replicaResponseCount = jedis.waitReplicas(replicaCount, waitingTimeMillis);
                retry++;
            }

            if (retry > retryCount) {
                String msg = String.format("Expected number of replica(s) is [%s] but available number of replica(s) is [%s]",
                        replicaCount, replicaResponseCount);
                throw new ReplicaIsDownException(msg);
            }
        }
    }

}
