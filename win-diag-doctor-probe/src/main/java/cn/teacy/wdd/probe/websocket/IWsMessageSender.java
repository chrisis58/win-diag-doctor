package cn.teacy.wdd.probe.websocket;

import cn.teacy.wdd.protocol.WsMessagePayload;

public interface IWsMessageSender {

    void send(WsMessagePayload payload);

}
