package cn.teacy.wdd.agent.utils;

import cn.teacy.wdd.agent.common.GraphKeys;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GraphUtils {

    public static KeyStrategyFactory buildKeyStrategyFactory(Class<? extends GraphKeys> clazz) {
        if (clazz == null || !clazz.isEnum()) {
            throw new IllegalArgumentException("Class must be an enum type");
        }

        var enumConstants = clazz.getEnumConstants();
        var strategyMap = java.util.Arrays.stream(enumConstants)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        GraphKeys::getKey,
                        GraphKeys::getStrategy,
                        (existing, replacement) -> existing
                ));

        return () -> strategyMap;
    }

}
