package cn.teacy.wdd.config;

import cn.teacy.wdd.config.interceptor.ProbeContextInterceptor;
import cn.teacy.wdd.config.interceptor.TaskResultAuthInterceptor;
import cn.teacy.wdd.protocol.WsMessageMapper;
import cn.teacy.wdd.protocol.WsProtocolHandlerRegistry;
import cn.teacy.wdd.support.*;
import com.alibaba.cloud.ai.agent.studio.controller.AgentController;
import com.alibaba.cloud.ai.agent.studio.controller.ThreadController;
import com.alibaba.cloud.ai.agent.studio.loader.AgentLoader;
import com.alibaba.cloud.ai.agent.studio.service.ThreadService;
import com.alibaba.cloud.ai.agent.studio.service.ThreadServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class GeneralConfig implements WebMvcConfigurer {

    private final ProbeContext probeContext;

    @Bean
    public WsMessageMapper wsPayloadExtractor(ObjectMapper objectMapper) {
        return new WsMessageMapper(objectMapper);
    }

    @Bean
    public WsProtocolHandlerRegistry wsProtocolHandlerRegistry(WsMessageMapper wsMessageMapper) {
        return new WsProtocolHandlerRegistry(wsMessageMapper, "cn.teacy.wdd.websocket.handler");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("redirect:/dashboard.html");
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new ProbeContextInterceptor(probeContext))
                .addPathPatterns("/chatui/index.html")
                .addPathPatterns("/run_sse");

        registry.addInterceptor(new TaskResultAuthInterceptor())
                .addPathPatterns("/task_result/**");
    }

    @Bean
    @Primary
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.createXmlMapper(false)
                .mixIn(Message.class, MessageMixIn.class)
                .mixIn(UserMessage.class, UserMessageMixIn.class)
                .mixIn(AssistantMessage.class, AssistantMessageMixIn.class)
                .mixIn(SystemMessage.class, SystemMessageMinIn.class)
                .build();
    }

    @Bean
    public XmlMapper xmlMapper() {
        XmlMapper mapper = new XmlMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return mapper;
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentController agentController(AgentLoader agentLoader) {
        return new AgentController(agentLoader);
    }

    @Bean
    @ConditionalOnMissingBean
    public ThreadService threadService() {
        return new ThreadServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public ThreadController threadController(ThreadService threadService) {
        return new ThreadController(threadService);
    }

}
