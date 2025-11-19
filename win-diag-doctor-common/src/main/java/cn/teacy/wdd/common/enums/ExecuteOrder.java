package cn.teacy.wdd.common.enums;

import java.util.Iterator;

public enum ExecuteOrder implements Comparable<ExecuteOrder> {

    FIRST,

    HALF_FIRST,

    COMMON,

    HALF_LAST,

    LAST;

    public static Iterator<ExecuteOrder> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < ExecuteOrder.values().length;
            }

            @Override
            public ExecuteOrder next() {
                return ExecuteOrder.values()[index++];
            }
        };
    }

}
