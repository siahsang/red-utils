package org.github.siahsang.redutils.replica;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
public interface ReplicaManager {
    void waitForResponse(int replicaCount, int waitingTimeMillis, int retryCount);
}
