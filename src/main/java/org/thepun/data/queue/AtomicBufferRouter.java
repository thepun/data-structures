package org.thepun.data.queue;

import java.util.Arrays;

import org.thepun.unsafe.ArrayMemory;
import org.thepun.unsafe.MemoryFence;

public final class AtomicBufferRouter<T> implements Router<T> {

    // TODO: align producers/consumers
    // TODO: get rid of size


    private final int size;
    private final int mask;
    private final long[] index;
    private final Object[] data;

    private AtomicBufferConsumer<T>[] consumers;
    private AtomicBufferProducer<T>[] producers;

    public AtomicBufferRouter(int bufferSize) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("Size should be greater then zero");
        }

        double log2 = Math.log10(bufferSize) / Math.log10(2);
        int pow = (int) Math.ceil(log2);

        size = (int) Math.pow(2, pow);
        mask = size - 1;
        data = new Object[size];
        index = new long[size * 8];
        consumers = new AtomicBufferConsumer[0];
        producers = new AtomicBufferProducer[0];

        for (int i = 0; i < size; i++) {
            ArrayMemory.setLong(index, i * 8, i - size + 4);
        }
    }

    @Override
    public synchronized QueueTail<T> createProducer() {
        AtomicBufferProducer<T>[] oldProducers = producers;
        AtomicBufferProducer<T>[] newProducers = Arrays.copyOf(oldProducers, oldProducers.length + 1);
        AtomicBufferProducer<T> producer = new AtomicBufferProducer<>(this);
        newProducers[oldProducers.length] = producer;
        updateProducers(newProducers);
        return producer;
    }

    @Override
    public synchronized QueueHead<T> createConsumer() {
        AtomicBufferConsumer<T>[] oldConsumers = consumers;
        AtomicBufferConsumer<T>[] newConsumers = Arrays.copyOf(oldConsumers, oldConsumers.length + 1);
        AtomicBufferConsumer<T> consumer = new AtomicBufferConsumer<>(this);
        newConsumers[oldConsumers.length] = consumer;
        updateConsumers(newConsumers);
        return consumer;
    }

    @Override
    public synchronized void destroyProducer(QueueTail<T> producer) {
        if (!(producer instanceof AtomicBufferProducer)) {
            throw new IllegalArgumentException("Wrong producer");
        }

        AtomicBufferProducer<T> producerSubqueue = (AtomicBufferProducer<T>) producer;
        if (producerSubqueue.parent != this) {
            throw new IllegalArgumentException("Producer from another router");
        }

        AtomicBufferProducer<T>[] newProducers;
        AtomicBufferProducer<T>[] oldProducers;

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

        newProducers = new AtomicBufferProducer[oldProducers.length - 1];
        System.arraycopy(oldProducers, 0, newProducers, 0, index);
        System.arraycopy(oldProducers, index + 1, newProducers, index + 1 - 1, oldProducers.length - (index + 1));
        updateProducers(newProducers);
    }

    @Override
    public synchronized void destroyConsumer(QueueHead<T> consumer) {
        if (!(consumer instanceof AtomicBufferConsumer)) {
            throw new IllegalArgumentException("Wrong consumer");
        }

        AtomicBufferConsumer<T> producerSubqueue = (AtomicBufferConsumer<T>) consumer;
        if (producerSubqueue.parent != this) {
            throw new IllegalArgumentException("Consumer from another router");
        }

        AtomicBufferConsumer<T>[] newConsumers;
        AtomicBufferConsumer<T>[] oldConsumers;

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

        newConsumers = new AtomicBufferConsumer[oldConsumers.length - 1];
        System.arraycopy(oldConsumers, 0, newConsumers, 0, index);
        System.arraycopy(oldConsumers, index + 1, newConsumers, index + 1 - 1, oldConsumers.length - (index + 1));
        updateConsumers(newConsumers);
    }

    private void updateProducers(AtomicBufferProducer<T>[] newProducers) {
        producers = newProducers;

        /*int count = newProducers.length;
        int step = size / count;
        for (int i = 0; i < count; i++) {
            newProducers[i].producerWriteCounter.set(step * i);
        }*/
    }

    private void updateConsumers(AtomicBufferConsumer<T>[] newConsumers) {
        consumers = newConsumers;

        /*int count = newConsumers.length;
        int step = size / count;
        for (int i = 0; i < count; i++) {
            newConsumers[i].consumerReadCounter.set(step * i);
        }*/
    }


    private static final class AtomicBufferProducer<T> implements QueueTail<T> {

        private final AtomicBufferRouter<T> parent;

        private final int size;
        private final int mask;
        private final long[] index;
        private final Object[] data;

        private AlignedLong producerWriteCounter;

        private AtomicBufferProducer(AtomicBufferRouter<T> parent) {
            this.parent = parent;

            size = parent.size;
            mask = parent.mask;
            data = parent.data;
            index = parent.index;

            producerWriteCounter = new AlignedLong();
            producerWriteCounter.set(0);
        }

        @Override
        public boolean addToTail(T element) {
            long localWriteCounter = producerWriteCounter.get();

            int i = (int) (localWriteCounter & mask);
            long k;
            for (;;) {
                k = ArrayMemory.getLong(index, i * 8);
                if (k < localWriteCounter - size + 3) {
                    return false;
                } else if (k == localWriteCounter - size + 3) {
                    continue;
                } else if (k == localWriteCounter - size + 4) {
                    if (ArrayMemory.compareAndSwapLong(index, i * 8, k, localWriteCounter + 1)) {
                        ArrayMemory.setObject(data, i, element);
                        MemoryFence.store();
                        ArrayMemory.setLong(index, i * 8, localWriteCounter + 2);
                        producerWriteCounter.set(localWriteCounter + 1);
                        return true;
                    }
                }

                localWriteCounter += 1;
                i = (int) (localWriteCounter & mask);
            }
        }
    }


    private static final class AtomicBufferConsumer<T> implements QueueHead<T> {

        private final AtomicBufferRouter<T> parent;

        private final int size;
        private final int mask;
        private final long[] index;
        private final Object[] data;

        private AlignedLong consumerReadCounter;

        private AtomicBufferConsumer(AtomicBufferRouter<T> parent) {
            this.parent = parent;

            size = parent.size;
            mask = parent.mask;
            data = parent.data;
            index = parent.index;

            consumerReadCounter = new AlignedLong();
            consumerReadCounter.set(0);
        }

        @Override
        public T removeFromHead() {
            long localReadCounter = consumerReadCounter.get();

            int i = (int) (localReadCounter & mask);
            long k;
            for (;;) {
                k = ArrayMemory.getLong(index, i * 8);
                if (k < localReadCounter) {
                    return null;
                } else if (k == localReadCounter + 1) {
                    continue;
                } else if (k == localReadCounter + 2) {
                    if (ArrayMemory.compareAndSwapLong(index, i * 8, k, localReadCounter + 3)) {
                        T element = (T) ArrayMemory.getObject(data, i);
                        MemoryFence.load();
                        ArrayMemory.setLong(index, i * 8, localReadCounter + 4);
                        consumerReadCounter.set(localReadCounter + 1);
                        return element;
                    }
                }

                localReadCounter += 1;
                i = (int) (localReadCounter & mask);
            }
        }
    }
}
