package cn.teacy.wdd.protocol.response;

import cn.teacy.wdd.common.entity.UserContext;
import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.WsProtocol;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@WsProtocol(identifier = "response:user:context:get")
public class GetUserContextResponse extends WsMessagePayload {

    UserContext userContext;

}
