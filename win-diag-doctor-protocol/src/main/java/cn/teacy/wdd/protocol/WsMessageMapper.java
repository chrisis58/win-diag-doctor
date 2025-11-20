package cn.teacy.wdd.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
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
public class WsMessageMapper {

    private static final String[] PACKAGES_TO_SCAN = {
            "cn.teacy.wdd.protocol.response",
            "cn.teacy.wdd.protocol.command",
            "cn.teacy.wdd.protocol.event"
    };

    private final Map<String, Class<? extends WsMessagePayload>> identifierMap = new HashMap<>();

    @Getter
    private Set<Class<? extends WsMessagePayload>> registeredPayloadClasses;

    private final ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
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

                    if (!WsMessagePayload.class.isAssignableFrom(clazz)) {
                        log.warn("类 {} 不是 WsMessagePayload 的子类，跳过注册", clazz.getName());
                        continue;
                    }

                    WsProtocol annotation = clazz.getAnnotation(WsProtocol.class);
                    if (annotation != null) {
                        String identifier = annotation.identifier();

                        if (identifierMap.containsKey(identifier)) {
                            log.error("发现重复的 WebSocket Identifier: [{}]", identifier);
                            throw new IllegalStateException("Duplicate identifier: " + identifier +
                                    " (Class: " + clazz.getName() + ")");
                        }

                        identifierMap.put(identifier, (Class<? extends WsMessagePayload>) clazz);
                        log.debug("注册消息类型: {} -> {}", identifier, clazz.getSimpleName());
                    }
                } catch (ClassNotFoundException | LinkageError e) {
                    log.warn("无法加载类: {}", beanDef.getBeanClassName(), e);
                }
            }
        }
        registeredPayloadClasses = Set.copyOf(identifierMap.values()); // unmodifiable
        log.info("WsMsg 扫描完成，共注册 {} 种消息类型", identifierMap.size());
    }

    @SuppressWarnings("unchecked")
    public WsMessage<? extends WsMessagePayload> read(String message) throws JsonProcessingException {

        JsonNode rootNode = objectMapper.readTree(message);

        JsonNode identifierNode = rootNode.get("identifier");
        if (identifierNode == null || identifierNode.isNull()) {
            log.warn("收到无效消息: 缺少 identifier 字段");
            return null;
        }
        String identifier = identifierNode.asText();

        Class<?> payloadClass = identifierMap.get(identifier);

        if (payloadClass == null) {
            log.warn("收到未知消息类型: {}, 无法解析 Payload", identifier);
            return objectMapper.treeToValue(rootNode, WsMessage.class);
        }

        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(WsMessage.class, payloadClass);

        try {
            return objectMapper.treeToValue(rootNode, javaType);
        } catch (IllegalArgumentException e) {
            log.error("JSON 解析失败，目标类型: WsMessage<{}>", payloadClass.getName(), e);
            throw new RuntimeException("Failed to parse message", e);
        }

    }

}
