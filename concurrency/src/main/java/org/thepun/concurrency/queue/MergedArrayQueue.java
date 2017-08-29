package org.thepun.concurrency.queue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MergedArrayQueue<T> implements QueueHead<T>, QueueTail<T> {

    private final int size;
    private final Object[] data;
    private final AlignedCAS counter;

    public MergedArrayQueue(int queueSize) {
        if (queueSize < 1) {
            throw new IllegalArgumentException("Size should be greater then zero");
        }

        size = queueSize;
        data = new Object[queueSize];
        counter = new AlignedCAS();
    }

    @Override
    public T removeFromHead() {
        int readIndex, writeIndex;
        long combinedIndex, newCombinedIndex;

        do {
            combinedIndex = counter.get();
            writeIndex = (int) (combinedIndex >> 32);
            readIndex = (int) combinedIndex;
            if (readIndex < writeIndex) {
                readIndex++;
                newCombinedIndex = ((long) writeIndex << 32) | readIndex;
            } else {
                return null;
            }
        } while (!counter.compareAndSwap(combinedIndex, newCombinedIndex));

        int index = readIndex % size;
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
        int readIndex, writeIndex;
        long combinedIndex, newCombinedIndex;

        do {
            combinedIndex = counter.get();
            writeIndex = (int) (combinedIndex >> 32);
            readIndex = (int) combinedIndex;
            if (writeIndex < readIndex + size) {
                writeIndex++;
                newCombinedIndex = ((long) writeIndex << 32) | readIndex;
            } else {
                return false;
            }
        } while (!counter.compareAndSwap(combinedIndex, newCombinedIndex));

        int index = writeIndex % size;
        data[index] = element;
        return true;
    }
}
