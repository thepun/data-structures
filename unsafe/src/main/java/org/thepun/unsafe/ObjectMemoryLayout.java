package org.thepun.unsafe;

public final class ObjectMemoryLayout {

    public static <T> long getFieldMemoryOffset(Class<T> type, String fieldName) {
        try {
            return UnsafeLocator.getUnsafe().objectFieldOffset(type.getDeclaredField(fieldName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
