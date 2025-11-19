package cn.teacy.wdd.protocol.response;

import cn.teacy.wdd.common.entity.WinEventLogEntry;
import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.WsProtocol;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@WsProtocol(identifier = "response:log:query")
public class LogQueryResponse extends WsMessagePayload {

    List<WinEventLogEntry> entries;

}
