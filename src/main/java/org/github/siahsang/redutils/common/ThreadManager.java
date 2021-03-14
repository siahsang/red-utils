package org.github.siahsang.redutils.common;

import java.util.UUID;
import java.util.concurrent.ThreadFactory;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
public class ThreadManager {
    private final static String GENERATED_UUID = UUID.randomUUID().toString();

    public static String getCurrentThreadName() {
        long threadId = Thread.currentThread().getId();
        return threadId + ":" + GENERATED_UUID;
    }

    public static ThreadFactory threadFactory() {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(new ThreadGroup(getCurrentThreadName()), r);
            }
        };
    }
}
