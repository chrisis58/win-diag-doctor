package cn.teacy.wdd.agent.common;

public class GraphInitializationException extends RuntimeException {
    public GraphInitializationException(String message) {
        super(message);
    }

    public GraphInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
