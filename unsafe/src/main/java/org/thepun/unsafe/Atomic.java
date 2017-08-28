package org.thepun.unsafe;

public final class Atomic {

    public static boolean compareAndSwapInt(Object object, long fieldOffset, int expectedValue, int newValue) {
        return UnsafeLocator.getUnsafe().compareAndSwapInt(object, fieldOffset, expectedValue, newValue);
    }

    public static boolean compareAndSwapLong(Object object, long fieldOffset, long expectedValue, long newValue) {
        return UnsafeLocator.getUnsafe().compareAndSwapLong(object, fieldOffset, expectedValue, newValue);
    }

    public static boolean compareAndSwapObject(Object object, long fieldOffset, Object expectedValue, Object newValue) {
        return UnsafeLocator.getUnsafe().compareAndSwapObject(object, fieldOffset, expectedValue, newValue);
    }

}
