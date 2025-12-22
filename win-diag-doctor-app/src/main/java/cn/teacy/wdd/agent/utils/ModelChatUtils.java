package cn.teacy.wdd.agent.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModelChatUtils {

    @NonNull
    public static String extractContent(ChatResponse response, @NonNull String defaultValue) {
        String content = Optional.ofNullable(Optional.ofNullable(response)
                        .orElseThrow(() -> new RuntimeException("chat response is null"))
                        .getResult()
                        .getOutput()
                        .getText())
                .orElseThrow(() -> new RuntimeException("chat response text is null"));

        if (content == null || content.isBlank()) {
            return defaultValue;
        }
        return content;
    }

}
