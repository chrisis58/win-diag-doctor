package cn.teacy.wdd.common.enums;

import lombok.Getter;

import java.util.Set;

/**
 * Windows 日志等级枚举
 *
 */
@Getter
public enum LogLevel {

    CRITICAL(Set.of(1)),
    ERROR(Set.of(2)),
    WARNING(Set.of(3)),
    INFORMATION(Set.of(0, 4)),
    VERBOSE(Set.of(5));

    private final Set<Integer> value;

    LogLevel(Set<Integer> value) {
        this.value = value;
    }

}