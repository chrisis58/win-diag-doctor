package cn.teacy.wdd.agent.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

@Slf4j
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

    public String read(PromptIdentifier promptIdentifier) {
        String identifier = promptIdentifier.getIdentifier();
        String fallback = promptIdentifier.getDefaultPrompt();
        String extension = promptIdentifier.getExtension();

        String path = String.format("classpath:prompts/%s.%s.%s", identifier, locale, extension);
        Resource resource = resourceLoader.getResource(path);

        if (!resource.exists()) {
            if (StringUtils.hasText(fallback)) {
                return fallback;
            }
            throw new RuntimeException("Prompt file not found and no fallback provided: " + path);
        }

        try {
            return resource.getContentAsString(Charset.defaultCharset());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return fallback;
        }
    }

    public PromptTemplate getTemplate(PromptIdentifier promptIdentifier) {
        String identifier = promptIdentifier.getIdentifier();
        String fallback = promptIdentifier.getDefaultPrompt();
        String extension = promptIdentifier.getExtension();

        String path = String.format("classpath:prompts/%s.%s.%s", identifier, locale, extension);
        Resource resource = resourceLoader.getResource(path);

        if (!resource.exists()) {
            if (StringUtils.hasText(fallback)) {
                return new PromptTemplate(fallback);
            }
            throw new RuntimeException("Prompt file not found and no fallback provided: " + path);
        }

        return new PromptTemplate(resource);
    }

    public String loadPrompt(PromptIdentifier promptIdentifier) {
        return render(promptIdentifier, Collections.emptyMap());
    }

    public String render(PromptIdentifier promptIdentifier, Map<String, Object> variables) {
        return getTemplate(promptIdentifier).render(variables);
    }

}
