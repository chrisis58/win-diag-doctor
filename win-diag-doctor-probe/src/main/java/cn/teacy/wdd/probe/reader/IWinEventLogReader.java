package cn.teacy.wdd.probe.reader;

import cn.teacy.wdd.common.entity.WinEventLogEntry;
import cn.teacy.wdd.protocol.command.LogQueryRequest;

import java.util.List;

/**
 * Windows事件日志读取器接口
 */
public interface IWinEventLogReader {

    /**
     * 读取指定日志名称和级别的事件日志条目
     * TODO: 后续添加更多过滤条件，如时间范围、来源等
     *
     * @param queryRequest 日志查询请求对象，包含日志名称和级别等过滤条件
     * @return 事件日志条目列表
     */
    List<WinEventLogEntry> readEventLogs(LogQueryRequest queryRequest);

}
