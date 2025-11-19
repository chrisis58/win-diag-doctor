package cn.teacy.wdd.protocol;

import cn.teacy.wdd.common.enums.ExecuteOrder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WsProtocolHandler {

    /**
     * 该处理器支持的协议类型
     */
    Class<? extends WsMessagePayload>[] protocols() default {};

    /**
     * 该处理器排除的协议类型
     */
    Class<? extends WsMessagePayload>[] excludeProtocols() default {};

    /**
     * 该处理器需要接管的处理器
     * 如果 A 接管了 B，那么同一条消息只会被 A 处理，避免重复处理
     */
    Class<? extends IWsProtocolHandler>[] takeovers() default {};

    /**
     * 该处理器的执行顺序
     * - {@link ExecuteOrder} 枚举定义了处理器的执行顺序
     */
    ExecuteOrder order() default ExecuteOrder.COMMON;

}
