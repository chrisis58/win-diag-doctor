package cn.teacy.wdd.agent.tools;

import com.felipestanzani.jtoon.EncodeOptions;
import com.felipestanzani.jtoon.JToon;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.lang.Nullable;

import java.lang.reflect.Type;

/**
 * Use TOON format to encode tool results.
 */
public class ToonResultConverter implements ToolCallResultConverter {

    private static final EncodeOptions DEFAULT_OPTIONS = new EncodeOptions(2, com.felipestanzani.jtoon.Delimiter.PIPE, true);

    @NotNull
    @Override
    public String convert(@Nullable Object result, @Nullable Type returnType) {
        return JToon.encode(result, DEFAULT_OPTIONS);
    }

}
