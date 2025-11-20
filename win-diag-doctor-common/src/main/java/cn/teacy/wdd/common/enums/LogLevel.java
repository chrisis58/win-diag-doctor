package cn.teacy.wdd.common.enums;

import lombok.Getter;

/**
 * Windows 日志等级枚举
 *
 */
@Getter
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

}