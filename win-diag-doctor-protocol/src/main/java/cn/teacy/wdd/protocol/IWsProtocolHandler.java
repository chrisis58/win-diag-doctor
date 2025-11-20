package cn.teacy.wdd.protocol;

public interface IWsProtocolHandler {

    void handle(String taskId, WsMessagePayload payload);

}
