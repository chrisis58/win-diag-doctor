package cn.teacy.wdd.protocol.exception;

public class ProtocolAnnotationAbsenceException extends RuntimeException {
    public ProtocolAnnotationAbsenceException(Class<?> clazz) {
        super(String.format("Class [%s] is missing @WsMsg annotation", clazz.getSimpleName()));
    }
}
