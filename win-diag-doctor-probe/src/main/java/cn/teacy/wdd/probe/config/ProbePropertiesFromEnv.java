package cn.teacy.wdd.probe.config;

public class ProbePropertiesFromEnv implements IProbeProperties {

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
