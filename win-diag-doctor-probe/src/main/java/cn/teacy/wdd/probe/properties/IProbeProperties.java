package cn.teacy.wdd.probe.properties;

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

    /**
     * 获取 WebSocket 服务器主机
     */
    String getWsServerHost();

}
