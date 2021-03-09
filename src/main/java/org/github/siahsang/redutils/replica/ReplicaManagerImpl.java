package org.github.siahsang.redutils.replica;

import org.github.siahsang.redutils.exception.ReplicaIsDownException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

public class ReplicaManagerImpl implements ReplicaManager {
    private static final Logger log = LoggerFactory.getLogger(ReplicaManagerImpl.class);

    private final int replicaCount;

    private final int waitingTimeForReplicasMillis;

    private final int retryCountForSyncingWithReplicas;


    public ReplicaManagerImpl(final int replicaCount,
                              final int waitingTimeForReplicasMillis,
                              final int retryCountForSyncingWithReplicas) {
        this.replicaCount = replicaCount;
        this.waitingTimeForReplicasMillis = waitingTimeForReplicasMillis;
        this.retryCountForSyncingWithReplicas = retryCountForSyncingWithReplicas;
    }

    @Override
    public void waitForReplicaResponse(final Jedis jedis) {
        if (replicaCount > 0) {
            int retryCount = 1;
            long replicaResponseCount = waitForReplicasToWrite(jedis, replicaCount, waitingTimeForReplicasMillis);

            while (replicaResponseCount != replicaCount && retryCount <= retryCountForSyncingWithReplicas) {
                log.warn("Expected number of replica(s) is [{}] but available number of replica(s) is [{}], trying again({})",
                        replicaCount, replicaResponseCount, retryCount);
                replicaResponseCount = waitForReplicasToWrite(jedis, replicaCount, waitingTimeForReplicasMillis);
                retryCount++;
            }

            if (retryCount > retryCountForSyncingWithReplicas) {
                String msg = String.format("Expected number of replica(s) is [%s] but available number of replica(s) is [%s] ",
                        replicaCount, replicaResponseCount);
                throw new ReplicaIsDownException(msg);
            }
        }
    }

    private long waitForReplicasToWrite(Jedis jedis, int replicaCount, int timeOutMillis) {
        return jedis.waitReplicas(replicaCount, timeOutMillis);
    }
}
