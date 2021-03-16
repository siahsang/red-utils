package org.github.siahsang.redutils.common;

import java.util.UUID;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
public abstract class ThreadManager {
    private ThreadManager() {
    }

    private static final InheritableThreadLocal<String> PARENT_THREAD_NAME = new InheritableThreadLocal<>();

    private static final String GENERATED_UUID = UUID.randomUUID().toString();

    public static String createUniqiueName() {
        long threadId = Thread.currentThread().getId();
        PARENT_THREAD_NAME.set(threadId + ":" + GENERATED_UUID);
        return PARENT_THREAD_NAME.get();
    }

    public static String getName() {
        return PARENT_THREAD_NAME.get();
    }

}
