package cn.teacy.wdd.common.enums;

import lombok.Getter;

@Getter
public enum ExecutionResultEndpoint {

    LogQuery("/api/tasks/log-result"),

    ;

    private final String endpoint;

    ExecutionResultEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

}
