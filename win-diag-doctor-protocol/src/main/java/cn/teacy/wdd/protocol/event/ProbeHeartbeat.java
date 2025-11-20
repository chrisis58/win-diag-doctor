package cn.teacy.wdd.protocol.event;

import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.WsProtocol;

@WsProtocol(identifier = "event:probe:heartbeat")
public class ProbeHeartbeat extends WsMessagePayload {

}
