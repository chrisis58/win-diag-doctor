package cn.teacy.wdd.agent.common;

import com.alibaba.cloud.ai.graph.KeyStrategy;

public interface GraphKeys {

    String getKey();

    KeyStrategy getStrategy();


    String WRAPPED_KEY_FORMAT = "{%s}";

    default String getWrappedKey() {
        return String.format(WRAPPED_KEY_FORMAT, getKey());
    }

}
