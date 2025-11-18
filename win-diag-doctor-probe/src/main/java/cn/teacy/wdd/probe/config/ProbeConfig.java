package cn.teacy.wdd.probe.config;

import cn.teacy.wdd.probe.properties.IProbeProperties;
import cn.teacy.wdd.probe.properties.ProbePropertiesFromEnv;
import cn.teacy.wdd.probe.properties.ProbePropertiesFromFile;
import cn.teacy.wdd.protocol.WsPayloadExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.*;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@ComponentScan("cn.teacy.wdd.probe")
public class ProbeConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    public WsPayloadExtractor wsPayloadExtractor(ObjectMapper objectMapper) {
        return new WsPayloadExtractor(objectMapper);
    }

    @Bean
    @Primary // only for suppressing warning
    @Conditional(ProbePropertiesFromEnv.AllProbeEnvVarsCondition.class)
    public IProbeProperties probePropertiesFromEnv() {
        return new ProbePropertiesFromEnv();
    }

    @Bean
    @Conditional(ProbePropertiesFromFile.MissingProbeEnvVarsCondition.class)
    public IProbeProperties probePropertiesFromFile() {
        return new ProbePropertiesFromFile();
    }

}
