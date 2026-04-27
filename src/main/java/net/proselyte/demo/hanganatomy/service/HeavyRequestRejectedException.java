package net.proselyte.demo.hanganatomy.service;

public class HeavyRequestRejectedException extends RuntimeException {

    public HeavyRequestRejectedException(String message) {
        super(message);
    }

    public HeavyRequestRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
