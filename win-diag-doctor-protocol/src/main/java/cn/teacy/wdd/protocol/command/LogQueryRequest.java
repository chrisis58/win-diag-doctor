package cn.teacy.wdd.protocol.command;

import cn.teacy.wdd.common.constants.LogLevel;
import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.WsProtocol;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@WsProtocol(identifier = "command:logs:query")
public class LogQueryRequest extends WsMessagePayload {

    /**
     * 要查询的日志名称
     *
     * @see cn.teacy.wdd.common.constants.LogNames
     */
    private String logName;

    /**
     * 要查询的日志级别
     *
     * @see cn.teacy.wdd.common.constants.LogLevel
     */
    private List<LogLevel> levels;

    /**
     * 最大事件数
     */
    private int maxEvents = 30;

    /**
     * 查询多早之前的日志，单位：小时
     *
     * @apiNote 应该传递正值，表示“多少小时前”，而不是负值
     */
    private Integer startHoursAgo;

    /**
     * 查询到多早之前的日志，单位：小时
     *
     * @apiNote 应该传递正值，表示“多少小时前”，而不是负值
     */
    private Integer endHoursAgo;

}
