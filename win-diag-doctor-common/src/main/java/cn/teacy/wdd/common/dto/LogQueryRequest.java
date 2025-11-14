package cn.teacy.wdd.common.dto;

import cn.teacy.wdd.common.constants.LogLevel;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 日志查询请求参数实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogQueryRequest {

    /**
     * 要查询的日志名称
     *
     * @see cn.teacy.wdd.common.constants.LogNames
     */
    @JsonProperty("logName")
    private String logName;

    /**
     * 要查询的日志级别
     *
     * @see cn.teacy.wdd.common.constants.LogLevel
     */
    @JsonProperty("levels")
    private List<LogLevel> levels;

    /**
     * 最大事件数
     */
    @JsonProperty("maxEvents")
    private int maxEvents = 30;

}
