package org.github.siahsang.redutils.common;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

public interface Scheduler {

    static <T> CompletableFuture<T> scheduleAtFixRate(ScheduledExecutorService executor, OperationCallBack operationCallBack,
                                                      final long initialDelay, final long delay, final TimeUnit unit) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();

        executor.scheduleAtFixedRate(() -> {
            try {
                operationCallBack.doOperation();
            } catch (Exception exception) {
                completableFuture.completeExceptionally(exception);
                throw exception;
            }

        }, initialDelay, delay, unit);

        return completableFuture;
    }

}
