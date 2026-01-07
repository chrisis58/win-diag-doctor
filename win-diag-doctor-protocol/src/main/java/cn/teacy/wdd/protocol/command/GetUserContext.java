package cn.teacy.wdd.protocol.command;

import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.WsProtocol;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@WsProtocol(identifier = "command:user:context:get")
public class GetUserContext extends WsMessagePayload {

}
