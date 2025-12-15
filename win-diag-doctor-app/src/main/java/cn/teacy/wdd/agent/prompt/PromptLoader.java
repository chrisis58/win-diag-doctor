package cn.teacy.wdd.agent.prompt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PromptLoader {

    private final ResourceLoader resourceLoader;
    private final String locale;

    public PromptLoader(
            ResourceLoader resourceLoader,
            @Value("${wdd.prompts.locale:zh}") String locale
    ) {
        this.resourceLoader = resourceLoader;
        this.locale = locale;
    }

    public String loadPrompt(String identifier, @Nullable String fallback) {
        String path = String.format("classpath:prompts/%s.%s.md", identifier, locale);
        Resource resource = resourceLoader.getResource(path);

        try {
            if (!resource.exists()) {
                if (fallback != null && !fallback.isEmpty()) {
                    return fallback;
                }
                throw new RuntimeException("Prompt file not found: " + path);
            }
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt for identifier: " + identifier, e);
        }
    }

    public String loadPrompt(PromptIdentifier promptIdentifier) {
        return loadPrompt(promptIdentifier.getIdentifier(), promptIdentifier.getDefaultPrompt());
    }

}
