package org.github.siahsang.redutils.exception;

/**
 * @author Javad Alimohammadi
 */

public class ReplicaIsDownException extends RuntimeException {
    public ReplicaIsDownException(String message) {
        super(message);
    }
}
