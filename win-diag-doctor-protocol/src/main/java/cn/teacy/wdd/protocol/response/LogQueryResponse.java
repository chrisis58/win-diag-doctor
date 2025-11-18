package cn.teacy.wdd.protocol.response;

import cn.teacy.wdd.common.entity.WinEventLogEntry;
import cn.teacy.wdd.protocol.WsProtocol;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@WsProtocol(identifier = "response:log:query")
public class LogQueryResponse {

    List<WinEventLogEntry> entries;

}
