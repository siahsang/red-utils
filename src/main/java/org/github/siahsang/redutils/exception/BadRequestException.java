package org.github.siahsang.redutils.exception;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
