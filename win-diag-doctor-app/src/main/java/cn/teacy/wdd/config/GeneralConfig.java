package cn.teacy.wdd.config;

import cn.teacy.wdd.common.interfaces.TaskIdGenerator;
import cn.teacy.wdd.config.interceptor.ProbeContextInterceptor;
import cn.teacy.wdd.protocol.WsMessageMapper;
import cn.teacy.wdd.protocol.WsProtocolHandlerRegistry;
import cn.teacy.wdd.support.ProbeContext;
import com.alibaba.cloud.ai.agent.studio.controller.AgentController;
import com.alibaba.cloud.ai.agent.studio.controller.ThreadController;
import com.alibaba.cloud.ai.agent.studio.loader.AgentLoader;
import com.alibaba.cloud.ai.agent.studio.service.ThreadService;
import com.alibaba.cloud.ai.agent.studio.service.ThreadServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class GeneralConfig implements WebMvcConfigurer {

    private final ProbeContext probeContext;

    @Bean
    public TaskIdGenerator taskIdGenerator() {
        return UUID.randomUUID()::toString;
    }

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
                .addPathPatterns("/run_sse");
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
