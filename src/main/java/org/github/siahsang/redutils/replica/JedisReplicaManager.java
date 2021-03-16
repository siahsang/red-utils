package org.github.siahsang.redutils.replica;

import org.github.siahsang.redutils.common.connection.JedisConnectionManager;
import org.github.siahsang.redutils.exception.ReplicaIsDownException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

public class JedisReplicaManager implements ReplicaManager {
    private static final Logger log = LoggerFactory.getLogger(JedisReplicaManager.class);

    private final JedisConnectionManager jedisConnectionManager;

    private final int replicaCount;

    private final int retryCount;

    private final int waitingTimeMillis;

    public JedisReplicaManager(JedisConnectionManager jedisConnectionManager, int replicaCount,
                               int retryCount, int waitingTimeMillis) {
        this.jedisConnectionManager = jedisConnectionManager;
        this.replicaCount = replicaCount;
        this.retryCount = retryCount;
        this.waitingTimeMillis = waitingTimeMillis;
    }

    @Override
    public void waitForResponse() {
        if (replicaCount > 0) {
            int retry = 1;
            long replicaResponseCount = jedisConnectionManager.doWithConnection(jedis -> {
                return jedis.waitReplicas(replicaCount, waitingTimeMillis);
            });


            while (replicaResponseCount != replicaCount && retry <= retryCount) {
                log.warn("Expected number of replica(s) is [{}] but available number of replica(s) is [{}], trying again({})",
                        replicaCount, replicaResponseCount, retry);
                replicaResponseCount = jedisConnectionManager.doWithConnection(jedis -> {
                    return jedis.waitReplicas(replicaCount, waitingTimeMillis);
                });
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
