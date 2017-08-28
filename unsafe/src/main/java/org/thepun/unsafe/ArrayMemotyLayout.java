package org.thepun.unsafe;

@SuppressWarnings("unchecked")
public final class ArrayMemotyLayout {

    public static <T> long getElementMemoryOffset(T[] array, int index) {
        return getElementMemoryOffset((Class<T[]>) array.getClass(), index);
    }

    public static <T> long getElementMemoryOffset(Class<T[]> arrayClass, int index) {
        return UnsafeLocator.getUnsafe().arrayBaseOffset(arrayClass) + getElementSize(arrayClass) * index;
    }

    public static <T> int getElementSize(T[] array) {
        return getElementSize((Class<T[]>) array.getClass());
    }

    public static <T> int getElementSize(Class<T[]> arrayClass) {
        return UnsafeLocator.getUnsafe().arrayIndexScale(arrayClass);
    }

}
