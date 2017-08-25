package org.thepun.unsafe;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

class UnsafeLocator {

    private static final Unsafe INSTANCE;
    static {
        try {
            Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
            singleoneInstanceField.setAccessible(true);
            INSTANCE = (Unsafe) singleoneInstanceField.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Unsafe getUnsafe() {
        return INSTANCE;
    }

}
