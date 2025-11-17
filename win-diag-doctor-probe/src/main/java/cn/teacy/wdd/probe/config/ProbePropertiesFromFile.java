package cn.teacy.wdd.probe.config;

import cn.teacy.wdd.common.constants.ProbeConstants;
import cn.teacy.wdd.common.exception.ProbeInitializationException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Properties;

import static cn.teacy.wdd.common.constants.ProbeConstants.CONFIG_FILE_NAME;

@Slf4j
public class ProbePropertiesFromFile implements IProbeProperties {

    private final Properties properties = new Properties();

    public ProbePropertiesFromFile() {
        File configFile;
        try {
            File codeSourceFile = new File(
                    ProbePropertiesFromFile.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );

            File baseDir = codeSourceFile.isFile() ? codeSourceFile.getParentFile() : codeSourceFile;
            configFile = new File(baseDir, CONFIG_FILE_NAME);

        } catch (URISyntaxException e) {
            log.error("无法解析代码源 (Code Source) 的路径", e);
            throw new ProbeInitializationException("Cannot determine base path", e);
        }

        if (!configFile.exists()) {
            log.error("致命错误：未找到配置文件 '{}'", configFile.getAbsolutePath());
            throw new ProbeInitializationException(CONFIG_FILE_NAME + " not found.");
        }

        try (InputStream input = new FileInputStream(configFile)) {
            properties.load(input);
        } catch (Exception e) {
            log.error("无法加载探针配置文件: {}", configFile.getAbsolutePath(), e);
            throw new ProbeInitializationException("Failed to load config", e);
        }

        if (getProbeId() == null || getProbeSecret() == null || getServerHost() == null) {
            log.error("致命错误：配置文件 '{}' 中缺少必要的配置项", configFile.getAbsolutePath());
            throw new ProbeInitializationException("Missing required config items in " + CONFIG_FILE_NAME);
        }

    }

    public String getProbeId() {
        return properties.getProperty(ProbeConstants.ConfigKeys.PROBE_ID);
    }

    public String getProbeSecret() {
        return properties.getProperty(ProbeConstants.ConfigKeys.PROBE_SECRET);
    }

    public String getServerHost() {
        return properties.getProperty(ProbeConstants.ConfigKeys.SERVER_URL);
    }

}
