package org.thepun.data.queue;

import java.util.Arrays;

import org.thepun.unsafe.ArrayMemory;
import org.thepun.unsafe.MemoryFence;

public final class GreedyRingBufferMultiplexer<T> implements Multiplexer<T> {

    // TODO: change 'writeIndex >= readIndex + size' to 'writeIndex - size >= readIndex'
    // TODO: align producers and variables
    // TODO: get rid of size field

    private final int size;
    private final int mask;
    private final Object[] data;
    private final AlignedLong readCounter;
    private final AlignedLong writeCounter;

    private AlignedLong[] producerCounters;
    private RingBufferProducer<T>[] producers;

    private long consumerReadCounter;
    private long consumerWriteCounter;

    public GreedyRingBufferMultiplexer(int bufferSize) {
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
        producerCounters = new AlignedLong[0];
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

    private void updateProducers(RingBufferProducer<T>[] newProducers) {
        producers = newProducers;

        AlignedLong[] newProducerCounters = new AlignedLong[newProducers.length];
        for (int i = 0; i < newProducers.length; i++) {
            newProducerCounters[i] = newProducers[i].producerWriteCounter;
        }

        producerCounters = newProducerCounters;
    }

    @Override
    public T removeFromHead() {
        int localMask = mask;
        Object[] localData = data;
        long writeIndex = consumerWriteCounter;
        AlignedLong localReadCounter = readCounter;

        long readIndex = consumerReadCounter;
        if (readIndex >= writeIndex) {
            AlignedLong[] localProducers = producerCounters;
            AlignedLong localWriteCounter = writeCounter;

            writeIndex = localWriteCounter.get();
            for (int i = 0; i < localProducers.length; i++) {
                long localWriteCounterFromConsumer = ArrayMemory.getObject(localProducers, i).get();
                if (writeIndex > localWriteCounterFromConsumer) {
                    writeIndex = localWriteCounterFromConsumer;
                }
            }
            consumerWriteCounter = writeIndex;

            if (readIndex >= writeIndex) {
                return null;
            }
        }

        int index = (int) (readIndex & localMask);
        Object element = ArrayMemory.getObject(localData, index);

        MemoryFence.load();
        long nextReadIndex = readIndex + 1;
        consumerReadCounter = nextReadIndex;
        localReadCounter.set(nextReadIndex);
        return (T) element;
    }


    private static final class RingBufferProducer<T> implements QueueTail<T> {

        private final GreedyRingBufferMultiplexer<T> parent;

        private final int size;
        private final int mask;
        private final Object[] data;
        private final AlignedLong readCounter;
        private final AlignedLong writeCounter;
        private final AlignedLong producerWriteCounter;

        private long localReadCounter;
        private long lastKnownWriteCounter;

        private RingBufferProducer(GreedyRingBufferMultiplexer<T> parent) {
            this.parent = parent;

            size = parent.size;
            mask = parent.mask;
            data = parent.data;
            readCounter = parent.readCounter;
            writeCounter = parent.writeCounter;

            producerWriteCounter = new AlignedLong();
            producerWriteCounter.set(Long.MAX_VALUE);

            lastKnownWriteCounter = 0;
        }

        @Override
        public boolean addToTail(T element) {
            long readIndex = localReadCounter;

            long writeIndex = producerWriteCounter.get();
            if (writeIndex == Long.MAX_VALUE) {
                producerWriteCounter.set(lastKnownWriteCounter);

                writeIndex = writeCounter.getAndIncrement();
                lastKnownWriteCounter = writeIndex;
                producerWriteCounter.set(writeIndex);
            }

            if (writeIndex >= readIndex + size) {
                readIndex = readCounter.get();
                localReadCounter = readIndex;

                if (writeIndex >= readIndex + size) {
                    return false;
                }
            }

            int index = (int) (writeIndex & mask);
            ArrayMemory.setObject(data, index, element);

            MemoryFence.store();
            producerWriteCounter.set(Long.MAX_VALUE);
            return true;
        }
    }
}
