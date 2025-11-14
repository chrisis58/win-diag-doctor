package cn.teacy.wdd.common.constants;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Windows 日志等级枚举
 *
 */
public enum LogLevel {

    CRITICAL(1),
    ERROR(2),
    WARNING(3),
    INFORMATION(4),
    VERBOSE(5);

    private final int value;

    LogLevel(int value) {
        this.value = value;
    }

    /**
     * 序列化时使用 int 值，而不是 "CRITICAL", "ERROR"。
     */
    @JsonValue
    public int getValue() {
        return value;
    }

}