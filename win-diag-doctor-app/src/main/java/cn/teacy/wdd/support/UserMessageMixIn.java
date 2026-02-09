package cn.teacy.wdd.support;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserMessageMixIn {
    @JsonCreator
    public UserMessageMixIn(@JsonProperty("text") String text) {}
}
