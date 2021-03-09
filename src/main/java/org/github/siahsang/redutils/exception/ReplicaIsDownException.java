package org.github.siahsang.redutils.exception;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

public class ReplicaIsDownException extends RuntimeException {
    public ReplicaIsDownException(String message) {
        super(message);
    }
}
