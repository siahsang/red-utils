package org.github.siahsang.redutils.common;

import java.util.UUID;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
public class ThreadManager {
    private final String uuid = UUID.randomUUID().toString();

    public String generateUniqueValue() {
        long threadId = Thread.currentThread().getId();
        return threadId + ":" + uuid;
    }
}
