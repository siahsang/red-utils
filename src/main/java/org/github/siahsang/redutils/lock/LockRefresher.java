package org.github.siahsang.redutils.lock;

import java.util.concurrent.CompletableFuture;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
public interface LockRefresher {
    CompletableFuture<Void> start(String lockName);

    void tryStop(String lockName);
}
