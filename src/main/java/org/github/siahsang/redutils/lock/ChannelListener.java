package org.github.siahsang.redutils.lock;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class hold information about subscribers for the channel
 *
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
public abstract class ChannelListener {

    private final AtomicBoolean hasNotification = new AtomicBoolean(false);

    private final Semaphore notificationResource = new Semaphore(0);

    private final Set<Long> subscribers = new HashSet<>();


    /**
     * Wait calling thread for getting notification from channel.
     *
     * @param timeOutMillis maximum amount of time for waiting to get
     * @throws InterruptedException
     */
    public void waitForGettingNotificationFromChannel(final long timeOutMillis) throws InterruptedException {

        boolean acquireNotificationRecourse = notificationResource.tryAcquire(timeOutMillis, TimeUnit.MILLISECONDS);

        // if true, this means we got a new message from the channel and we consumed it
        if (acquireNotificationRecourse) {
            hasNotification.set(false);
        }

    }

    public void onGettingNewMessage() {
        if (hasNotification.compareAndSet(false, true)) {
            notificationResource.release();
        }
    }

    public void addSubscriber(long subscriberId) {
        subscribers.add(subscriberId);
    }

    public void removeSubscriber(long subscriberId) {
        subscribers.remove(subscriberId);
    }

    public boolean isSubscribersEmpty() {
        return subscribers.isEmpty();
    }

    public abstract void shutdown();

    public abstract void startListening();
}
