package org.github.siahsang.redutils.lock;

/**
 * @author Javad Alimohammadi
 */
public interface LockChannel {
    void subscribe(String lockName);

    /**
     * Wait for getting the notification of releasing the lock
     *
     * @param lockName
     * @param timeOutMillis
     * @throws InterruptedException
     */
    void waitForNotification(String lockName, long timeOutMillis) throws InterruptedException;

    void unSubscribe(String lockName);
}
