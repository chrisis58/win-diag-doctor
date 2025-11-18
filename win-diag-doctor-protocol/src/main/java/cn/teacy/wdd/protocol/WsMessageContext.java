package cn.teacy.wdd.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Deprecated
@NoArgsConstructor
@AllArgsConstructor
public class WsMessageContext {

    private Object request;

    private Object response;

}
