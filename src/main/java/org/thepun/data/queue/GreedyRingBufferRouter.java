package org.thepun.data.queue;

import java.util.Arrays;

import org.thepun.unsafe.ArrayMemory;
import org.thepun.unsafe.MemoryFence;

public final class GreedyRingBufferRouter<T> implements Router<T> {

    private final int size;
    private final int mask;
    private final Object[] data;
    private final AlignedLong readCounter;
    private final AlignedLong writeCounter;

    private RingBufferConsumer<T>[] consumers;
    private RingBufferProducer<T>[] producers;

    public GreedyRingBufferRouter(int bufferSize) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("Size should be greater then zero");
        }

        double log2 = Math.log10(bufferSize) / Math.log10(2);
        int pow = (int) Math.ceil(log2);

        size = (int) Math.pow(2, pow);
        mask = size - 1;
        data = new Object[size];
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

        private final GreedyRingBufferRouter<T> parent;

        private final int size;
        private final int mask;
        private final Object[] data;
        private final AlignedLong readCounter;
        private final AlignedLong writeCounter;
        private final AlignedLong localWriteCounter;

        private long localReadCounter;

        private RingBufferConsumer<T>[] consumers;

        private RingBufferProducer(GreedyRingBufferRouter<T> parent) {
            this.parent = parent;

            size = parent.size;
            mask = parent.mask;
            data = parent.data;
            consumers = parent.consumers;
            readCounter = parent.readCounter;
            writeCounter = parent.writeCounter;

            localWriteCounter = new AlignedLong();
            localWriteCounter.set(Long.MAX_VALUE);
        }

        @Override
        public boolean addToTail(T element) {
            long readIndex = localReadCounter;

            long writeIndex = writeCounter.get();
            if (writeIndex >= readIndex + size) {
                RingBufferConsumer<T>[] localConsumers = consumers;

                readIndex = readCounter.get();
                for (int i = 0; i < localConsumers.length; i++) {
                    long localReadCounterFromConsumer = localConsumers[i].localReadCounter.get();
                    if (readIndex > localReadCounterFromConsumer) {
                        readIndex = localReadCounterFromConsumer;
                    }
                }
                localReadCounter = readIndex;

                if (writeIndex >= readIndex + size) {
                    return false;
                }
            }

            MemoryFence.store();
            localWriteCounter.set(writeIndex);

            while (!writeCounter.compareAndSwap(writeIndex, writeIndex + 1)) {
                writeIndex = writeCounter.get();

                if (writeIndex >= readIndex + size) {
                    localWriteCounter.set(Long.MAX_VALUE);
                    return false;
                }
            }

            int index = (int) (writeIndex & mask);
            ArrayMemory.setObject(data, index, element);

            MemoryFence.store();
            localWriteCounter.set(Long.MAX_VALUE);
            return true;
        }
    }


    private static final class RingBufferConsumer<T> implements QueueHead<T> {

        private final GreedyRingBufferRouter<T> parent;

        private final int mask;
        private final Object[] data;
        private final AlignedLong readCounter;
        private final AlignedLong writeCounter;
        private final AlignedLong localReadCounter;

        private long localWriteCounter;

        private RingBufferProducer<T>[] producers;

        private RingBufferConsumer(GreedyRingBufferRouter<T> parent) {
            this.parent = parent;

            mask = parent.mask;
            data = parent.data;
            producers = parent.producers;
            readCounter = parent.readCounter;
            writeCounter = parent.writeCounter;

            localReadCounter = new AlignedLong();
            localReadCounter.set(Long.MAX_VALUE);
        }

        @Override
        public T removeFromHead() {
            long writeIndex = localWriteCounter;

            long readIndex = localReadCounter.get();
            if (readIndex == Long.MAX_VALUE) {
                readIndex = readCounter.getAndIncrement();
                localReadCounter.set(readIndex);
            }

            if (readIndex >= writeIndex) {
                RingBufferProducer<T>[] localProducers = producers;

                writeIndex = writeCounter.get();
                for (int i = 0; i < localProducers.length; i++) {
                    long localWriteCounterFromConsumer = localProducers[i].localWriteCounter.get();
                    if (writeIndex > localWriteCounterFromConsumer) {
                        writeIndex = localWriteCounterFromConsumer;
                    }
                }
                localWriteCounter = writeIndex;

                if (readIndex >= writeIndex) {
                    return null;
                }
            }

            int index = (int) (readIndex & mask);
            Object element = ArrayMemory.getObject(data, index);

            MemoryFence.store();
            localReadCounter.set(Long.MAX_VALUE);
            return (T) element;
        }
    }
}
