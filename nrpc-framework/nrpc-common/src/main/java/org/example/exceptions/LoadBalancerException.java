package org.example.exceptions;

/**
 * @author xiaonaol
 * @date 2024/11/20
 **/
public class LoadBalancerException extends RuntimeException {
    public LoadBalancerException() {
    }

    public LoadBalancerException(Throwable cause) {
        super(cause);
    }

    public LoadBalancerException(String message) {
    }
}
