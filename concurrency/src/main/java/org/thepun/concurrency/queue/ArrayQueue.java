package org.thepun.concurrency.queue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ArrayQueue<T> implements QueueHead<T>, QueueTail<T> {

    private final int size;
    private final Object[] data;
    private final AlignedCounter readCounter;
    private final AlignedCounter writeCounter;

    public ArrayQueue(int queueSize) {
        if (queueSize < 1) {
            throw new IllegalArgumentException("Size should be greater then zero");
        }

        size = queueSize;
        data = new Object[queueSize];
        readCounter = new AlignedCounter();
        writeCounter = new AlignedCounter();
    }

    @Override
    public T removeFromHead() {
        long writeIndex = writeCounter.get();
        long readIndex = readCounter.tryIncrement(writeIndex);
        if (readIndex == -1) {
            return null;
        }

        int index = (int) readIndex % size;
        return (T) data[index];
    }

    @Override
    public T removeFromHead(long timeout, TimeUnit timeUnit) throws TimeoutException, InterruptedException {
        // TODO: implement busy wait
        return null;
    }

    @Override
    public boolean addToTail(T element) {
        long readIndex = readCounter.get();
        long writeIndex = writeCounter.tryIncrement(readIndex + size);
        if (writeIndex == -1) {
            return false;
        }

        int index = (int) writeIndex % size;
        data[index] = element;
        return true;
    }
}
