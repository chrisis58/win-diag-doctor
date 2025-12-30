package cn.teacy.wdd.protocol.command;

import cn.teacy.wdd.common.enums.LogLevel;
import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.WsProtocol;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;

import java.util.List;

@Data
@Builder
@ToString
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
    @JsonPropertyDescription("Log name (e.g. Application, Security)")
    private String logName;

    /**
     * 要查询的日志级别
     *
     * @see LogLevel
     */
    @JsonPropertyDescription("List of log levels")
    private List<LogLevel> levels;

    /**
     * 最大事件数
     */
    @JsonPropertyDescription("Max events limit, defaults to 10")
    private int maxEvents = 10;

    /**
     * 查询多早之前的日志，单位：小时
     *
     * @apiNote 应该传递正值，表示“多少小时前”，而不是负值
     */
    @JsonPropertyDescription("Query logs starting from how many hours ago, should be a positive value")
    private Integer startHoursAgo;

    /**
     * 查询到多早之前的日志，单位：小时
     *
     * @apiNote 应该传递正值，表示“多少小时前”，而不是负值
     */
    @JsonPropertyDescription("Query logs up to how many hours ago, should be a positive value")
    private Integer endHoursAgo;

}
