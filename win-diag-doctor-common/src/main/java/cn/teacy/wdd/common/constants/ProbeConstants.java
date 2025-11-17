package cn.teacy.wdd.common.constants;

public interface ProbeConstants {

    /**
     * 探针配置文件名称
     */
    String CONFIG_FILE_NAME = "probe.properties";

    interface ConfigKeys {

        /**
         * 服务器地址
         */
        String SERVER_URL = "server.host";

        /**
         * 探针 ID
         */
        String PROBE_ID = "wddp.id";

        /**
         * 探针密钥
         */
        String PROBE_SECRET = "wddp.secret";

    }

}
