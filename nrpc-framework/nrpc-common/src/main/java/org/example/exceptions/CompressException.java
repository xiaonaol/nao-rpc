package org.example.exceptions;

/**
 * @author xiaonaol
 * @date 2024/11/20
 **/
public class CompressException extends RuntimeException {
    public CompressException() {
    }

    public CompressException(Throwable cause) {
        super(cause);
    }

    public CompressException(String message) {
    }
}
