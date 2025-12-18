package cn.teacy.wdd.common.interfaces;

import java.util.concurrent.atomic.AtomicLong;

@FunctionalInterface
public interface StringIdGenerator {

    String generateId();

    class DefaultIdGenerator implements StringIdGenerator {
        private final AtomicLong COUNTER = new AtomicLong(1);
        private final String prefix;

        public DefaultIdGenerator() {
            this.prefix = null;
        }

        public DefaultIdGenerator(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String generateId() {
            if (prefix != null) {
                return String.format("%s%05d", prefix, COUNTER.getAndIncrement());
            }

            return String.format("%05d", COUNTER.getAndIncrement());
        }
    }

}
