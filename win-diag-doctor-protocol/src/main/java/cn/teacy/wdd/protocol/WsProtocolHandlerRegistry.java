package cn.teacy.wdd.protocol;

import cn.teacy.wdd.common.enums.ExecuteOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class WsProtocolHandlerRegistry implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    private final WsMessageMapper wsMessageMapper;
    private final String basePackage;

    private final Map<Class<? extends WsMessagePayload>, Map<ExecuteOrder, Set<IWsProtocolHandler>>> protocolHandlers = new HashMap<>();
    private final Map<Class<? extends WsMessagePayload>, Set<Class<? extends IWsProtocolHandler>>> protocolHandlersTakenOver = new HashMap<>();

    @SuppressWarnings("unchecked")
    public void register(Class<? extends WsProtocolHandler> handlerClass) {
        assert handlerClass.isAnnotationPresent(WsProtocolHandler.class): "Handler class must be annotated with @WsProtocolHandler";
        assert IWsProtocolHandler.class.isAssignableFrom(handlerClass): "Handler class must implement IWsProtocolHandler interface";

        WsProtocolHandler annotation = handlerClass.getAnnotation(WsProtocolHandler.class);
        Class<? extends WsMessagePayload>[] protocols = annotation.protocols();
        List<Class<? extends WsMessagePayload>> excludeProtocols = Arrays.asList(annotation.excludeProtocols());
        Class<? extends IWsProtocolHandler>[] takeovers = annotation.takeovers();
        ExecuteOrder order = annotation.order();

        // 如果没有指定协议类，则注册所有协议类
        if (protocols.length == 0) {
            protocols = wsMessageMapper.getRegisteredPayloadClasses().toArray(Class[]::new);
        }

        // 获取实例
        IWsProtocolHandler handler = (IWsProtocolHandler) applicationContext.getBean(handlerClass);

        // 注册
        for (Class<? extends WsMessagePayload> protocolClass : protocols) {
            if (excludeProtocols.contains(protocolClass)) {
                continue;
            }

            // 如果处理器被接管，则不注册
            if (protocolHandlersTakenOver.getOrDefault(protocolClass, Collections.emptySet()).contains(handlerClass)) {
                continue;
            }

            // 注册接管
            for (Class<? extends IWsProtocolHandler> takeover : takeovers) {
                // 向对应的协议类的黑名单中添加被接管的处理器
                protocolHandlersTakenOver.computeIfAbsent(protocolClass, k -> new HashSet<>()).add(takeover);
                // 删除被接管的处理器
                protocolHandlers.computeIfPresent(protocolClass, (k, v) -> {
                    v.values().forEach(handlers -> handlers.removeIf(takeover::isInstance));
                    return v;
                });
            }

            protocolHandlers.computeIfAbsent(protocolClass, k -> new HashMap<>())
                    .computeIfAbsent(order, k -> new HashSet<>()).add(handler);
        }

    }

    public Map<ExecuteOrder, Set<IWsProtocolHandler>> getHandlers(Class<?> protocolClass) {
        return protocolHandlers.getOrDefault(protocolClass, Collections.emptyMap());
    }

    @SuppressWarnings("unchecked")
    @EventListener(ContextRefreshedEvent.class)
    public void scanAndRegisterAllClasses() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(WsProtocolHandler.class));

        // 扫描项目路径，尝试注册协议类
        for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());

                if (!clazz.isAnnotationPresent(WsProtocolHandler.class)) {
                    continue;
                }

                register((Class<? extends WsProtocolHandler>) clazz);
            } catch (Exception e) {
                log.error("Error while scanning class: {}", bd.getBeanClassName(), e);
            }
        }

        log.info("Successfully registered handlers for {} protocol types.", protocolHandlers.size());
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
