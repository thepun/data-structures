package org.thepun.data.transfer;

import java.util.Arrays;

import org.thepun.unsafe.ArrayMemory;
import org.thepun.unsafe.MemoryFence;

public final class RingBufferRouter<T> implements HasProducers<T>, HasConsumers<T> {

    // TODO: extend from AbstractRouter
    // TODO: change 'writeIndex >= readIndex + size' to 'writeIndex - size >= readIndex'
    // TODO: use unsafe array access
    // TODO: get rid of size field

    private final int size;
    private final int mask;
    private final Object[] data;
    private final AlignedLong readCounter;
    private final AlignedLong writeCounter;

    private AlignedLong[] consumerCounters;
    private AlignedLong[] producerCounters;
    private RingBufferConsumer<T>[] consumers;
    private RingBufferProducer<T>[] producers;

    public RingBufferRouter(int bufferSize) {
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
        consumerCounters = new AlignedLong[0];
        producerCounters = new AlignedLong[0];
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

        AlignedLong[] newProducerCounters = new AlignedLong[newProducers.length];
        for (int i = 0; i < newProducers.length; i++) {
            newProducerCounters[i] = newProducers[i].producerWriteCounter;
        }

        producerCounters = newProducerCounters;
        for (int i = 0; i < consumers.length; i++) {
            consumers[i].producers = newProducerCounters;
        }
    }

    private void updateConsumers(RingBufferConsumer<T>[] newConsumers) {
        consumers = newConsumers;

        AlignedLong[] newConsumerCounters = new AlignedLong[newConsumers.length];
        for (int i = 0; i < newConsumers.length; i++) {
            newConsumerCounters[i] = newConsumers[i].consumerReadCounter;
        }

        consumerCounters = newConsumerCounters;
        for (int i = 0; i < producers.length; i++) {
            producers[i].consumers = newConsumerCounters;
        }
    }


    private static final class RingBufferProducer<T> implements QueueTail<T> {

        private final int size;
        private final int mask;
        private final Object[] data;
        private final AlignedLong writeCounter;
        private final AlignedLong producerWriteCounter;
        private final AlignedLong readCounter;

        private AlignedLong[] consumers;
        private long producerReadCounter;

        private final RingBufferRouter<T> parent;

        // gap
        private Object t1, t2;

        private RingBufferProducer(RingBufferRouter<T> parent) {
            this.parent = parent;

            size = parent.size;
            mask = parent.mask;
            data = parent.data;
            consumers = parent.consumerCounters;
            readCounter = parent.readCounter;
            writeCounter = parent.writeCounter;

            producerWriteCounter = new AlignedLong();
            producerWriteCounter.set(Long.MAX_VALUE);
        }

        @Override
        public boolean addToTail(T element) {
            int localSize = size;
            long readIndex = producerReadCounter;
            int localMask = mask;
            Object[] localData = data;
            AlignedLong localWriteCounter = writeCounter;
            AlignedLong localProducerWriteCounter = producerWriteCounter;

            long writeIndex = localWriteCounter.get();
            long readIndexPlusSize = readIndex + localSize;
            if (writeIndex >= readIndexPlusSize) {
                AlignedLong[] localConsumers = consumers;
                AlignedLong localReadCounter = readCounter;

                readIndex = localReadCounter.get();
                for (int i = 0; i < localConsumers.length; i++) {
                    long localReadCounterFromConsumer = localConsumers[i].get();
                    if (readIndex > localReadCounterFromConsumer) {
                        readIndex = localReadCounterFromConsumer;
                    }
                }
                producerReadCounter = readIndex;

                readIndexPlusSize = readIndex + localSize;
                if (writeIndex >= readIndexPlusSize) {
                    return false;
                }
            }

            MemoryFence.store();
            localProducerWriteCounter.set(writeIndex);
            while (!localWriteCounter.compareAndSwap(writeIndex, writeIndex + 1)) {
                writeIndex = localWriteCounter.get();

                if (writeIndex >= readIndexPlusSize) {
                    localProducerWriteCounter.set(Long.MAX_VALUE);
                    return false;
                }
            }

            int index = (int) (writeIndex & localMask);
            ArrayMemory.setObject(localData, index, element);
            MemoryFence.store();

            localProducerWriteCounter.set(Long.MAX_VALUE);
            return true;
        }
    }


    private static final class RingBufferConsumer<T> implements QueueHead<T> {

        // gap
        private int t1;

        private final int mask;
        private final Object[] data;
        private final AlignedLong readCounter;
        private final AlignedLong consumerReadCounter;
        private final AlignedLong writeCounter;

        private long consumerWriteCounter;
        private AlignedLong[] producers;

        private final RingBufferRouter<T> parent;

        // gap
        private Object t2, t3, t4;

        private RingBufferConsumer(RingBufferRouter<T> parent) {
            this.parent = parent;

            mask = parent.mask;
            data = parent.data;
            producers = parent.producerCounters;
            readCounter = parent.readCounter;
            writeCounter = parent.writeCounter;

            consumerReadCounter = new AlignedLong();
            consumerReadCounter.set(Long.MAX_VALUE);
        }

        @Override
        public T removeFromHead() {
            int localMask = mask;
            Object[] localData = data;
            long writeIndex = consumerWriteCounter;
            AlignedLong localReadCounter = readCounter;
            AlignedLong localConsumerReadCounter = consumerReadCounter;

            long readIndex = localReadCounter.get();
            if (readIndex >= writeIndex) {
                AlignedLong[] localProducers = producers;
                AlignedLong localWriteCounter = writeCounter;

                writeIndex = localWriteCounter.get();
                for (int i = 0; i < localProducers.length; i++) {
                    long localWriteCounterFromConsumer = localProducers[i].get();
                    if (writeIndex > localWriteCounterFromConsumer) {
                        writeIndex = localWriteCounterFromConsumer;
                    }
                }
                consumerWriteCounter = writeIndex;

                if (readIndex >= writeIndex) {
                    return null;
                }
            }

            MemoryFence.store();
            localConsumerReadCounter.set(readIndex);
            while (!localReadCounter.compareAndSwap(readIndex, readIndex + 1)) {
                readIndex = localReadCounter.get();

                if (readIndex >= writeIndex) {
                    localConsumerReadCounter.set(Long.MAX_VALUE);
                    return null;
                }
            }

            int index = (int) (readIndex & localMask);
            Object element = ArrayMemory.getObject(localData, index);
            MemoryFence.load();

            localConsumerReadCounter.set(Long.MAX_VALUE);
            return (T) element;
        }
    }
}
