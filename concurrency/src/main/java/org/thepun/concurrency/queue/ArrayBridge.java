package org.thepun.concurrency.queue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ArrayBridge<T> implements QueueHead<T>, QueueTail<T> {

    private final int size;
    private final Object[] data;
    private final AlignedLong readCounter;
    private final AlignedLong writeCounter;

    public ArrayBridge(int queueSize) {
        if (queueSize < 1) {
            throw new IllegalArgumentException("Size should be greater then zero");
        }

        size = queueSize;
        data = new Object[queueSize];
        readCounter = new AlignedLong();
        writeCounter = new AlignedLong();
    }

    @Override
    public T removeFromHead() {
        long writeIndex = writeCounter.get();
        long readIndex = readCounter.get();
        if (readIndex >= writeIndex) {
            return null;
        }

        int index = (int) readIndex % size;
        Object element;
        do {
            element = data[index];
        } while (element == null);

        readCounter.set(readIndex + 1);

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
        long writeIndex = writeCounter.get();
        if (writeIndex >= readIndex + size) {
            return false;
        }

        int index = (int) writeIndex % size;
        data[index] = element;

        writeCounter.set(writeIndex + 1);

        return true;
    }
}
