package org.github.siahsang.redutils.exception;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

public class RefreshLockException extends RuntimeException {
    public RefreshLockException(String msg, Throwable cause) {
        super(cause);
    }
}
