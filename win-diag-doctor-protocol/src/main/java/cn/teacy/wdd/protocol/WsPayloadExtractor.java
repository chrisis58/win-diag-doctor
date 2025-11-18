package cn.teacy.wdd.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class WsPayloadExtractor {

    private static final String[] PACKAGES_TO_SCAN = {
            "cn.teacy.wdd.protocol.response",
            "cn.teacy.wdd.protocol.command"
    };

    private final Map<String, Class<?>> identifierMap = new HashMap<>();

    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        log.debug("开始扫描 WsMsg 消息类型...");

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(WsProtocol.class));

        ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

        for (String basePackage : PACKAGES_TO_SCAN) {
            Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);

            for (BeanDefinition beanDef : candidateComponents) {
                try {
                    String className = beanDef.getBeanClassName();
                    if (className == null) continue;

                    Class<?> clazz = ClassUtils.forName(className, classLoader);

                    WsProtocol annotation = clazz.getAnnotation(WsProtocol.class);
                    if (annotation != null) {
                        String identifier = annotation.identifier();

                        if (identifierMap.containsKey(identifier)) {
                            log.error("发现重复的 WebSocket Identifier: [{}]", identifier);
                            throw new IllegalStateException("Duplicate identifier: " + identifier +
                                    " (Class: " + clazz.getName() + ")");
                        }

                        identifierMap.put(identifier, clazz);
                        log.debug("注册消息类型: {} -> {}", identifier, clazz.getSimpleName());
                    }
                } catch (ClassNotFoundException | LinkageError e) {
                    log.warn("无法加载类: {}", beanDef.getBeanClassName(), e);
                }
            }
        }
        log.info("WsMsg 扫描完成，共注册 {} 种消息类型", identifierMap.size());
    }

    public Object extract(WsMessage message) {

        Class<?> targetClass = identifierMap.get(message.getIdentifier());

        if (targetClass == null) {
            log.warn("收到未知消息类型: {}, 无法解析 Payload", message.getIdentifier());
            return message.getPayload();
        }

        try {
            return objectMapper.convertValue(message.getPayload(), targetClass);
        } catch (IllegalArgumentException e) {
            log.error("Payload 转换失败，目标类型: {}", targetClass.getName(), e);
            throw new RuntimeException("Failed to convert message data", e);
        }

    }

}
