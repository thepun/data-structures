package org.thepun.data.queue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.thepun.unsafe.ArrayMemory;
import org.thepun.unsafe.MemoryFence;

public final class RingBufferRouter<T> implements Router<T> {

    private final int size;
    private final Object[] data;
    private final AlignedLong readCounter;
    private final AlignedLong writeCounter;

    private RingBufferConsumer<T>[] consumers;
    private RingBufferProducer<T>[] producers;

    public RingBufferRouter(int bufferSize) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("Size should be greater then zero");
        }

        size = bufferSize;
        data = new Object[bufferSize];
        readCounter = new AlignedLong();
        writeCounter = new AlignedLong();
        consumers = new RingBufferConsumer[0];
        producers = new RingBufferProducer[0];
    }

    @Override
    public synchronized QueueTail<T> createProducer() {
        RingBufferProducer<T>[] oldProducers = producers;
        RingBufferProducer<T>[] newProducers = Arrays.copyOf(oldProducers, oldProducers.length + 1);
        RingBufferProducer<T> producer = new RingBufferProducer<>(this);
        newProducers[oldProducers.length] = producer;
        updateProducers(newProducers);
        return producer;
    }

    @Override
    public synchronized QueueHead<T> createConsumer() {
        RingBufferConsumer<T>[] oldConsumers = consumers;
        RingBufferConsumer<T>[] newConsumers = Arrays.copyOf(oldConsumers, oldConsumers.length + 1);
        RingBufferConsumer<T> consumer = new RingBufferConsumer<>(this);
        newConsumers[oldConsumers.length] = consumer;
        updateConsumers(newConsumers);
        return consumer;
    }

    @Override
    public synchronized void destroyProducer(QueueTail<T> producer) {
        if (!(producer instanceof RingBufferProducer)) {
            throw new IllegalArgumentException("Wrong producer");
        }

        RingBufferProducer<T> producerSubqueue = (RingBufferProducer<T>) producer;
        if (producerSubqueue.parent != this) {
            throw new IllegalArgumentException("Producer from another router");
        }

        RingBufferProducer<T>[] newProducers;
        RingBufferProducer<T>[] oldProducers;

        oldProducers = producers;
        int index = -1;
        for (int i = 0; i < oldProducers.length; i++) {
            if (oldProducers[i] == producer) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            throw new IllegalArgumentException("Producer not found");
        }

        newProducers = new RingBufferProducer[oldProducers.length - 1];
        System.arraycopy(oldProducers, 0, newProducers, 0, index);
        System.arraycopy(oldProducers, index + 1, newProducers, index + 1 - 1, oldProducers.length - (index + 1));
        updateProducers(newProducers);
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
        updateConsumers(newConsumers);
    }

    private void updateProducers(RingBufferProducer<T>[] newProducers) {
        MemoryFence.full();
        producers = newProducers;
        for (int i = 0; i < consumers.length; i++) {
            consumers[i].producers = newProducers;
        }
    }

    private void updateConsumers(RingBufferConsumer<T>[] newConsumers) {
        consumers = newConsumers;
        for (int i = 0; i < producers.length; i++) {
            producers[i].consumers = newConsumers;
        }
    }


    private static final class RingBufferProducer<T> implements QueueTail<T> {

        private final RingBufferRouter<T> parent;

        private final int size;
        private final Object[] data;
        private final AlignedLong readCounter;
        private final AlignedLong writeCounter;

        private long localReadCounter;
        private long localWriteCounter;

        private RingBufferConsumer<T>[] consumers;

        private RingBufferProducer(RingBufferRouter<T> parent) {
            this.parent = parent;

            size = parent.size;
            data = parent.data;
            consumers = parent.consumers;
            readCounter = parent.readCounter;
            writeCounter = parent.writeCounter;
        }

        @Override
        public boolean addToTail(T element) {
            long readIndex = localReadCounter;

            long writeIndex = writeCounter.get();
            if (writeIndex >= readIndex + size) {
                RingBufferConsumer<T>[] localConsumers = this.consumers;

                readIndex = readCounter.get();
                for (int i = 0; i < localConsumers.length; i++) {
                    long localReadCounterFromConsumer = localConsumers[i].localReadCounter;
                    if (readIndex > localReadCounterFromConsumer) {
                        readIndex = localReadCounterFromConsumer;
                    }
                }
                localReadCounter = readIndex;

                if (writeIndex >= readIndex + size) {
                    localWriteCounter = Long.MAX_VALUE;
                    return false;
                }
            }

            localWriteCounter = writeIndex;
            while (!writeCounter.compareAndSwap(writeIndex, writeIndex + 1)) {
                writeIndex = writeCounter.get();

                if (writeIndex >= readIndex + size) {
                    localWriteCounter = Long.MAX_VALUE;
                    return false;
                }
            }

            int index = (int) writeIndex % size;
            ArrayMemory.setObject(data, index, element);
            MemoryFence.store();

            localWriteCounter = Long.MAX_VALUE;
            return true;
        }
    }


    private static final class RingBufferConsumer<T> implements QueueHead<T> {

        private final RingBufferRouter<T> parent;

        private final int size;
        private final Object[] data;
        private final AlignedLong readCounter;
        private final AlignedLong writeCounter;

        private long localReadCounter;
        private long localWriteCounter;

        private RingBufferProducer<T>[] producers;

        private RingBufferConsumer(RingBufferRouter<T> parent) {
            this.parent = parent;

            size = parent.size;
            data = parent.data;
            producers = parent.producers;
            readCounter = parent.readCounter;
            writeCounter = parent.writeCounter;
        }

        @Override
        public T removeFromHead() {
            long writeIndex = localWriteCounter;

            long readIndex = readCounter.get();
            if (readIndex >= writeIndex) {
                RingBufferProducer<T>[] localProducers = this.producers;

                writeIndex = writeCounter.get();
                for (int i = 0; i < localProducers.length; i++) {
                    long localWriteCounterFromConsumer = localProducers[i].localWriteCounter;
                    if (writeIndex > localWriteCounterFromConsumer) {
                        writeIndex = localWriteCounterFromConsumer;
                    }
                }
                localWriteCounter = writeIndex;

                if (readIndex >= writeIndex) {
                    localReadCounter = Long.MAX_VALUE;
                    return null;
                }
            }

            localReadCounter = readIndex;
            while (!readCounter.compareAndSwap(readIndex, readIndex + 1)) {
                readIndex = readCounter.get();

                if (readIndex >= writeIndex) {
                    localReadCounter = Long.MAX_VALUE;
                    return null;
                }
            }

            int index = (int) readIndex % size;
            Object element = ArrayMemory.getObject(data, index);
            MemoryFence.load();

            localReadCounter = Long.MAX_VALUE;
            return (T) element;
        }
    }
}
