package net.proselyte.demo.hanganatomy.service;

public class DependencyTimeoutException extends RuntimeException {

    public DependencyTimeoutException(String message) {
        super(message);
    }

    public DependencyTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
