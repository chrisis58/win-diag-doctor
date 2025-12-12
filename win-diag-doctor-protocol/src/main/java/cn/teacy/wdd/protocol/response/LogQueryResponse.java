package cn.teacy.wdd.protocol.response;

import cn.teacy.wdd.common.entity.UserContext;
import cn.teacy.wdd.common.entity.WinEventLogEntry;
import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.WsProtocol;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@WsProtocol(identifier = "response:log:query")
public class LogQueryResponse extends WsMessagePayload {

    UserContext userContext;

    /**
     * 是否有更多日志条目可供查询（相同条件）
     */
    boolean hasMore;

    /**
     * 根据查询请求返回的事件日志条目列表
     */
    List<WinEventLogEntry> entries;


    public static LogQueryResponse empty(UserContext userContext) {
        return new LogQueryResponse(userContext, false, Collections.emptyList());
    }

}
