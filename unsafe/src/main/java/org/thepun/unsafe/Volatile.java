package org.thepun.unsafe;

public final class Volatile {

    public static int getInt(Object object, long fieldOffset) {
        return UnsafeLocator.getUnsafe().getIntVolatile(object, fieldOffset);
    }

    public static long getLong(Object object, long fieldOffset) {
        return UnsafeLocator.getUnsafe().getLongVolatile(object, fieldOffset);
    }

    public static Object getObject(Object object, long fieldOffset) {
        return UnsafeLocator.getUnsafe().getObjectVolatile(object, fieldOffset);
    }

    public static void setInt(Object object, long fieldOffset, int newValue) {
        UnsafeLocator.getUnsafe().putIntVolatile(object, fieldOffset, newValue);
    }

    public static void setLong(Object object, long fieldOffset, long newValue) {
        UnsafeLocator.getUnsafe().putLongVolatile(object, fieldOffset, newValue);
    }

    public static void setObject(Object object, long fieldOffset, Object newValue) {
        UnsafeLocator.getUnsafe().putObjectVolatile(object, fieldOffset, newValue);
    }

}
