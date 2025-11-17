package cn.teacy.wdd.common.exception;

public class ProbeInitializationException extends RuntimeException {
    public ProbeInitializationException(String message) {
        super(message);
    }

    public ProbeInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
