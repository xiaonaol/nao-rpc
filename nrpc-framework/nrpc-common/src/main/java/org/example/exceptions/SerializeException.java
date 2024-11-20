package org.example.exceptions;

/**
 * @author xiaonaol
 * @date 2024/11/20
 **/
public class SerializeException extends RuntimeException {
    public SerializeException() {
    }

    public SerializeException(Throwable cause) {
        super(cause);
    }

    public SerializeException(String message) {
    }
}
