package cn.teacy.wdd.support;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SystemMessageMinIn {
    @JsonCreator
    public SystemMessageMinIn(@JsonProperty("text") String text) {}
}
