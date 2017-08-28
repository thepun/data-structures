package org.thepun.unsafe;

public final class ArrayMemory {

    private static final long OBJECT_OFFSET = ArrayMemotyLayout.getElementMemoryOffset(Object[].class, 0);
    private static final int OBJECT_SIZE = ArrayMemotyLayout.getElementSize(Object[].class);

    public static Object getObject(Object[] array, int index) {
        return getObject(array, OBJECT_OFFSET + OBJECT_SIZE * index);
    }

    public static void setObject(Object[] array, int index, Object element) {
        setObject(array, OBJECT_OFFSET + OBJECT_SIZE * index, element);
    }

    public static Object getObject(Object[] array, long offset) {
        return UnsafeLocator.getUnsafe().getObject(array, offset);
    }

    public static void setObject(Object[] array, long offset, Object element) {
        UnsafeLocator.getUnsafe().putObject(array, offset, element);
    }
}
