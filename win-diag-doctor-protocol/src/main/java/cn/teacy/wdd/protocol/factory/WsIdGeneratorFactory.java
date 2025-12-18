package cn.teacy.wdd.protocol.factory;

import java.util.ServiceLoader;

public class WsIdGeneratorFactory {

    private static final WsMessageIdGenerator INSTANCE;

    static {
        ServiceLoader<WsMessageIdGenerator> loader = ServiceLoader.load(WsMessageIdGenerator.class);
        WsMessageIdGenerator generator = null;

        for (WsMessageIdGenerator g : loader) {
            generator = g;
            break;
        }

        if (generator == null) {
            generator = new DefaultWsIdGenerator();
        }

        INSTANCE = generator;
    }

    public static WsMessageIdGenerator getInstance() {
        return INSTANCE;
    }

}
