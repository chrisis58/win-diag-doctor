package cn.teacy.wdd.agent.tools;

import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.EncodeOptions;
import dev.toonformat.jtoon.JToon;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.lang.Nullable;

import java.lang.reflect.Type;

/**
 * Use TOON format to encode tool results.
 */
public class ToonResultConverter implements ToolCallResultConverter {

    public static final EncodeOptions DEFAULT_OPTIONS = new EncodeOptions(2, Delimiter.PIPE, true, false, Integer.MAX_VALUE);

    @NotNull
    @Override
    public String convert(@Nullable Object result, @Nullable Type returnType) {
        return JToon.encode(result, DEFAULT_OPTIONS);
    }

}
