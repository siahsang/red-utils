package org.github.siahsang.redutils;

import org.github.siahsang.redutils.common.OperationCallBack;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
public interface RedUtilsLock {
    /**
     * Execute given operation when getting lock successfully from redis.
     *
     * @param lockName          Name of the lock
     * @param operationCallBack Operation that should be executed after acquiring lock successfully
     * @return True if getting lock successfully, false otherwise
     */
    boolean tryAcquire(String lockName, OperationCallBack operationCallBack);

    /**
     * Execute given operation when getting lock successfully from redis. Wait for getting lock if necessary
     *
     * @param lockName          Name of the lock
     * @param operationCallBack Operation that should be executed after acquiring lock successfully
     */
    void acquire(String lockName, OperationCallBack operationCallBack);
}
