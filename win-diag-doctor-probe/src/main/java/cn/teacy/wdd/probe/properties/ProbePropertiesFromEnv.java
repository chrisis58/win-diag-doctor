package cn.teacy.wdd.probe.properties;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

public class ProbePropertiesFromEnv implements IProbeProperties {

    public static class AllProbeEnvVarsCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
            Environment env = context.getEnvironment();

            return env.containsProperty("WDD_SERVER_HOST")
                    && env.containsProperty("WDD_PROBE_SECRET")
                    && env.containsProperty("WDD_PROBE_ID");
        }
    }

    @Override
    public String getServerHost() {
        return System.getenv("WDD_SERVER_HOST");
    }

    @Override
    public String getProbeSecret() {
        return System.getenv("WDD_PROBE_SECRET");
    }

    @Override
    public String getProbeId() {
        return System.getenv("WDD_PROBE_ID");
    }
}
