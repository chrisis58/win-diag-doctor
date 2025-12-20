package cn.teacy.wdd.agent.prompt;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;

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

    public PromptTemplate getTemplate(PromptIdentifier promptIdentifier) {
        String identifier = promptIdentifier.getIdentifier();
        String fallback = promptIdentifier.getDefaultPrompt();

        String path = String.format("classpath:prompts/%s.%s.md", identifier, locale);
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
