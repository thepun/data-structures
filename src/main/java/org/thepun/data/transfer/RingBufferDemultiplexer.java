package org.thepun.data.transfer;

import java.util.Arrays;

import org.thepun.unsafe.ArrayMemory;
import org.thepun.unsafe.MemoryFence;

public final class RingBufferDemultiplexer<T> implements QueueTail<T>, HasConsumers<T> {

    // TODO: align consumers
    // TODO: change size to mask
    // TODO: use array of counters
    // TODO: get rid of size field

    private final int size;
    private final Object[] data;
    private final AlignedLong readCounter;
    private final AlignedLong writeCounter;

    private long producerReadIndex;
    // TODO: split consumers and counters
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
        consumers = newConsumers;
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
        AlignedLong localWriteCounter = writeCounter;

        long readIndex = producerReadIndex;
        long writeIndex = localWriteCounter.get();
        long writeIndexMinusSize = writeIndex - size;
        if (writeIndexMinusSize >= readIndex) {
            RingBufferConsumer<T>[] localConsumers = consumers;
            int length = localConsumers.length;

            readIndex = readCounter.get();
            for (int i = 0; i < length; i++) {
                long localReadCounterFromConsumer = localConsumers[i].consumerReadCounter.get();
                if (readIndex > localReadCounterFromConsumer) {
                    readIndex = localReadCounterFromConsumer;
                }
            }
            producerReadIndex = readIndex;

            if (writeIndexMinusSize >= readIndex) {
                return false;
            }
        } else {
            producerReadIndex = readIndex + 1;
        }

        int index = (int) writeIndex % size;
        ArrayMemory.setObject(data, index, element);
        MemoryFence.store();

        localWriteCounter.set(writeIndex + 1);
        return true;
    }


    private static final class RingBufferConsumer<T> implements QueueHead<T> {

        private final RingBufferDemultiplexer<T> parent;

        private final int size;
        private final Object[] data;
        private final AlignedLong readCounter;
        private final AlignedLong writeCounter;
        private final AlignedLong consumerReadCounter;

        private RingBufferConsumer(RingBufferDemultiplexer<T> parent) {
            this.parent = parent;

            size = parent.size;
            data = parent.data;
            readCounter = parent.readCounter;
            writeCounter = parent.writeCounter;

            consumerReadCounter = new AlignedLong();
        }

        @Override
        public T removeFromHead() {
            AlignedLong localReadCounter = readCounter;
            AlignedLong localConsumerReadCounter = consumerReadCounter;

            long writeIndex = writeCounter.get();
            long readIndex = localReadCounter.get();
            if (readIndex >= writeIndex) {
                localConsumerReadCounter.set(Long.MAX_VALUE);
                return null;
            }

            localConsumerReadCounter.set(readIndex);
            while (!localReadCounter.compareAndSwap(readIndex, readIndex + 1)) {
                readIndex = localReadCounter.get();
                //writeIndex = writeCounter.get();

                if (readIndex >= writeIndex) {
                    localConsumerReadCounter.set(Long.MAX_VALUE);
                    return null;
                }
            }

            int index = (int) readIndex % size;
            Object element = ArrayMemory.getObject(data, index);
            MemoryFence.load();

            localConsumerReadCounter.set(Long.MAX_VALUE);
            return (T) element;
        }
    }
}
