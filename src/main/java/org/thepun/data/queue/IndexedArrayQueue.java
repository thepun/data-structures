package org.thepun.data.queue;

public final class IndexedArrayQueue<T> implements QueueHead<T>, QueueTail<T> {

    private static final Object DATA_REF = new Object();
    private static final Object EMPTY_REF = new Object();
    private static final Object ALMOST_DATA_REF = new Object();
    private static final Object ALMOST_EMPTY_REF = new Object();



    private final int size;
    private final int mask;
    private final Object[] data;

    public IndexedArrayQueue(int bufferSize) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("Size should be greater then zero");
        }

        double log2 = Math.log10(bufferSize) / Math.log10(2);
        int pow = (int) Math.ceil(log2);

        size = (int) Math.pow(2, pow);
        mask = size - 1;
        data = new Object[size * 2];

    }

    @Override
    public T removeFromHead() {
        return null;
    }

    @Override
    public boolean addToTail(T element) {
        return false;
    }
}
