package cn.teacy.wdd.probe.config;

public interface IProbeProperties {

    /**
     * 获取探针 ID
     */
    String getProbeId();

    /**
     * 获取探针密钥 (用于认证)
     */
    String getProbeSecret();

    /**
     * 获取服务器主机
     */
    String getServerHost();

}
