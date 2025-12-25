package cn.teacy.wdd.agent.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.Optional;

@Slf4j
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

    @NonNull
    @SuppressWarnings("unchecked")
    public static <O> O extractOutput(ChatResponse response, @NonNull O defaultValue, ObjectMapper objectMapper) {
        String rawText = extractContent(response, "");

        if (rawText.isBlank()) {
            return defaultValue;
        }

        String jsonText = cleanJsonBlock(rawText);

        try {
            return (O) objectMapper.readValue(jsonText, defaultValue.getClass());

        } catch (JsonProcessingException e) {
            log.warn("Failed to parse LLM output to [{}]. Raw content: [{}]. Returning default value.",
                    defaultValue.getClass().getSimpleName(), rawText, e);
            return defaultValue;
        } catch (Exception e) {
            log.error("Unexpected error during LLM output extraction.", e);
            throw new RuntimeException("LLM output extraction failed", e);
        }
    }

    private static String cleanJsonBlock(String text) {
        if (text == null) return "";
        String cleaned = text.trim();

        if (cleaned.startsWith("```")) {
            int newlineIndex = cleaned.indexOf("\n");
            if (newlineIndex != -1) {
                cleaned = cleaned.substring(newlineIndex + 1);
            } else {
                cleaned = cleaned.replace("```json", "").replace("```", "");
            }
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

}
