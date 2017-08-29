package org.thepun.concurrency.queue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ArrayQueue<T> implements QueueHead<T>, QueueTail<T> {

    private final int size;
    private final Object[] data;
    private final AlignedCAS readCounter;
    private final AlignedCAS writeCounter;

    public ArrayQueue(int queueSize) {
        if (queueSize < 1) {
            throw new IllegalArgumentException("Size should be greater then zero");
        }

        size = queueSize;
        data = new Object[queueSize];
        readCounter = new AlignedCAS();
        writeCounter = new AlignedCAS();
    }

    @Override
    public T removeFromHead() {
        long writeIndex = writeCounter.get();
        long readIndex = readCounter.tryGetAndIncrement(writeIndex);
        if (readIndex == -1) {
            return null;
        }

        int index = (int) readIndex % size;
        Object element;
        do {
            element = data[index];
        } while (element == null);

        return (T) element;
    }

    @Override
    public T removeFromHead(long timeout, TimeUnit timeUnit) throws TimeoutException, InterruptedException {
        // TODO: implement busy wait
        return null;
    }

    @Override
    public boolean addToTail(T element) {
        long readIndex = readCounter.get();
        long writeIndex = writeCounter.tryGetAndIncrement(readIndex + size);
        if (writeIndex == -1) {
            return false;
        }

        int index = (int) writeIndex % size;
        data[index] = element;
        return true;
    }
}
