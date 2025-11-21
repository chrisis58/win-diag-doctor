package cn.teacy.wdd.probe.properties;

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

@Getter
@ToString
public class ProbePropertiesFromEnv implements IProbeProperties {

    public static class AllProbeEnvVarsCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
            Environment env = context.getEnvironment();

            return env.containsProperty("WDD_SERVER_HOST")
                    && env.containsProperty("WDD_PROBE_SECRET")
                    && env.containsProperty("WDD_PROBE_ID")
                    && env.containsProperty("WDD_WS_SERVER_HOST");
        }
    }

    public static final String KEY_HOST = "WDD_SERVER_HOST";
    public static final String KEY_WS_HOST = "WDD_WS_SERVER_HOST";
    public static final String KEY_ID = "WDD_PROBE_ID";
    public static final String KEY_SECRET = "WDD_PROBE_SECRET";

    private final String serverHost;
    private final String wsServerHost;
    private final String probeId;

    @ToString.Exclude
    private final String probeSecret;

    /**
     * 构造函数：在实例化时一次性从环境变量读取
     */
    public ProbePropertiesFromEnv() {
        this.serverHost = System.getenv(KEY_HOST);
        this.wsServerHost = System.getenv(KEY_WS_HOST);
        this.probeId = System.getenv(KEY_ID);
        this.probeSecret = System.getenv(KEY_SECRET);
    }

}
