package cn.teacy.wdd.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "wdd") // 对应配置文件中的 wdd
public class WddProperties {

    private Ai ai = new Ai();
    private Probe probe = new Probe();

    @Data
    public static class Ai {
        private String baseUrl;
        private String apiKey;
        private String defaultModel;
        private String thinkModel;
        private String flashModel;
        private Duration timeout;
    }

    @Data
    public static class Probe {
        private String templatePath;
        private String connectKey;
    }
}