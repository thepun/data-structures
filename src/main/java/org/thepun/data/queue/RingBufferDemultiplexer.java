package org.thepun.data.queue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.thepun.unsafe.ArrayMemory;
import org.thepun.unsafe.MemoryFence;

public final class RingBufferDemultiplexer<T> implements Demultiplexer<T> {

    private final int size;
    private final Object[] data;
    private final AlignedLong readCounter;
    private final AlignedLong writeCounter;

    private long localReadCounter;

    private RingBufferConsumer<T>[] consumers;

    public RingBufferDemultiplexer(int bufferSize) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("Size should be greater then zero");
        }

        size = bufferSize;
        data = new Object[bufferSize];
        readCounter = new AlignedLong();
        writeCounter = new AlignedLong();
        consumers = new RingBufferConsumer[0];
    }

    @Override
    public synchronized QueueHead<T> createConsumer() {
        RingBufferConsumer<T>[] oldConsumers = consumers;
        RingBufferConsumer<T>[] newConsumers = Arrays.copyOf(oldConsumers, oldConsumers.length + 1);
        RingBufferConsumer<T> consumer = new RingBufferConsumer<>(this);
        newConsumers[oldConsumers.length] = consumer;
        MemoryFence.full();
        consumers = newConsumers;
        MemoryFence.full();
        return consumer;
    }

    @Override
    public synchronized void destroyConsumer(QueueHead<T> consumer) {
        if (!(consumer instanceof RingBufferConsumer)) {
            throw new IllegalArgumentException("Wrong consumer");
        }

        RingBufferConsumer<T> producerSubqueue = (RingBufferConsumer<T>) consumer;
        if (producerSubqueue.parent != this) {
            throw new IllegalArgumentException("Consumer from another router");
        }

        RingBufferConsumer<T>[] newConsumers;
        RingBufferConsumer<T>[] oldConsumers;

        oldConsumers = consumers;
        int index = -1;
        for (int i = 0; i < oldConsumers.length; i++) {
            if (oldConsumers[i] == consumer) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            throw new IllegalArgumentException("Consumer not found");
        }

        newConsumers = new RingBufferConsumer[oldConsumers.length - 1];
        System.arraycopy(oldConsumers, 0, newConsumers, 0, index);
        System.arraycopy(oldConsumers, index + 1, newConsumers, index + 1 - 1, oldConsumers.length - (index + 1));
        consumers = newConsumers;
    }

    @Override
    public boolean addToTail(T element) {
        long readIndex = localReadCounter;

        long writeIndex = writeCounter.get();
        long writeIndexMinusSize = writeIndex - size;

        if (writeIndexMinusSize >= readIndex) {
            RingBufferConsumer<T>[] localConsumers = consumers;
            int length = localConsumers.length;

            readIndex = readCounter.get();
            for (int i = 0; i < length; i++) {
                long localReadCounterFromConsumer = localConsumers[i].localReadCounter.get();
                if (readIndex > localReadCounterFromConsumer) {
                    readIndex = localReadCounterFromConsumer;
                }
            }
            localReadCounter = readIndex;

            if (writeIndexMinusSize >= readIndex) {
                return false;
            }
        } else {
            localReadCounter = readIndex + 1;
        }

        int index = (int) writeIndex % size;
        ArrayMemory.setObject(data, index, element);
        MemoryFence.store();

        writeCounter.set(writeIndex + 1);
        return true;
    }


    private static final class RingBufferConsumer<T> implements QueueHead<T> {

        private final RingBufferDemultiplexer<T> parent;

        private final int size;
        private final Object[] data;
        private final AlignedLong readCounter;
        private final AlignedLong writeCounter;
        private final AlignedLong localReadCounter;

        private RingBufferConsumer(RingBufferDemultiplexer<T> parent) {
            this.parent = parent;

            size = parent.size;
            data = parent.data;
            readCounter = parent.readCounter;
            writeCounter = parent.writeCounter;

            localReadCounter = new AlignedLong();
        }

        @Override
        public T removeFromHead() {
            long writeIndex = writeCounter.get();

            long readIndex = readCounter.get();
            if (readIndex >= writeIndex) {
                localReadCounter.set(Long.MAX_VALUE);
                return null;
            }

            MemoryFence.load();
            int index = (int) readIndex % size;
            Object element = ArrayMemory.getObject(data, index);

            localReadCounter.set(readIndex);
            while (!readCounter.compareAndSwap(readIndex, readIndex + 1)) {
                readIndex = readCounter.get();
                //writeIndex = writeCounter.get();

                if (readIndex >= writeIndex) {
                    localReadCounter.set(Long.MAX_VALUE);
                    return null;
                }
            }

            //MemoryFence.load();

            localReadCounter.set(Long.MAX_VALUE);
            return (T) element;
        }

        @Override
        public T removeFromHead(long timeout, TimeUnit timeUnit) throws TimeoutException, InterruptedException {
            //TODO: implement busy wait
            return null;
        }
    }
}
