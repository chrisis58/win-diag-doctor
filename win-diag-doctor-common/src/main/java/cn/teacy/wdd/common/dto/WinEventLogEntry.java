package cn.teacy.wdd.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Windows事件日志条目
 *
 */
@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class WinEventLogEntry {

    /**
     * 事件 ID, 例如: 7000, 1001, 4207
     */
    @JsonProperty("Id")
    private int eventId;

    /**
     * 事件级别, 例如: "错误", "警告", "信息"
     */
    @JsonProperty("LevelDisplayName")
    private String level;

    /**
     * 事件来源, 例如: "Service Control Manager", "Tcpip", "VBoxNetLwf"
     */
    @JsonProperty("ProviderName")
    private String providerName;

    /**
     * 事件创建时间 (JSON 返回的是 /Date(1763084761257)/ 格式)
     * 我们暂时将其作为字符串接收，AI 可以理解这个格式，
     * 或者我们稍后在服务端进行转换。
     */
    @JsonProperty("TimeCreated")
    private String timeCreated;

    /**
     * 事件的核心消息文本
     */
    @JsonProperty("Message")
    private String message;

}
