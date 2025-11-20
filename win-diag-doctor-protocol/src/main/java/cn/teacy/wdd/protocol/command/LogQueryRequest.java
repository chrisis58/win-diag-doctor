package cn.teacy.wdd.protocol.command;

import cn.teacy.wdd.common.enums.LogLevel;
import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.WsProtocol;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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
    @JsonPropertyDescription("需要查询的日志名称，例如 `Application`, `System`, `Security` 等。")
    private String logName;

    /**
     * 要查询的日志级别
     *
     * @see LogLevel
     */
    @JsonPropertyDescription("需要查询的日志级别列表")
    private List<LogLevel> levels;

    /**
     * 最大事件数
     */
    @JsonPropertyDescription("最大事件数，默认10")
    private int maxEvents = 10;

    /**
     * 查询多早之前的日志，单位：小时
     *
     * @apiNote 应该传递正值，表示“多少小时前”，而不是负值
     */
    @JsonPropertyDescription("查询多早之前的日志，单位：小时，应该传递正值，表示`多少小时前开始`")
    private Integer startHoursAgo;

    /**
     * 查询到多早之前的日志，单位：小时
     *
     * @apiNote 应该传递正值，表示“多少小时前”，而不是负值
     */
    @JsonPropertyDescription("查询到多早之前的日志，单位：小时，应该传递正值，表示`多少小时前结束`")
    private Integer endHoursAgo;

}
