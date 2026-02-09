package cn.teacy.wdd.support;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class AssistantMessageMixIn {
    @JsonCreator
    public AssistantMessageMixIn(@JsonProperty("text") String text) {}
}
