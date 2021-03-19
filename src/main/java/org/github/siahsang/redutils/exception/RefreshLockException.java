package org.github.siahsang.redutils.exception;

/**
 * @author Javad Alimohammadi
 */

public class RefreshLockException extends RuntimeException {
    public RefreshLockException(String msg, Throwable cause) {
        super(msg,cause);
    }
}
