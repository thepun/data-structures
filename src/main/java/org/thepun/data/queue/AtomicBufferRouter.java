package org.thepun.data.queue;

import java.util.Arrays;

import org.thepun.unsafe.ArrayMemory;
import org.thepun.unsafe.MemoryFence;

public final class AtomicBufferRouter<T> implements Router<T> {

    // TODO: align producers/consumers
    // TODO: get rid of size


    private static final Object DATA = new Object();
    private static final Object EMPTY = new Object();
    private static final Object PULLING = new Object();
    private static final Object PUSHING = new Object();


    private final int size;
    private final int mask;
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
        data = new Object[size * 2];
        consumers = new AtomicBufferConsumer[0];
        producers = new AtomicBufferProducer[0];

        for (int i = 0; i < size; i++) {
            ArrayMemory.setObject(data, i * 2, EMPTY);
            ArrayMemory.setObject(data, i * 2 + 1, null);
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

        for (int i = 0; i < consumers.length; i++) {
            consumers[i].producers = newProducers;
        }

        /*int count = newProducers.length;
        int step = size / count;
        for (int i = 0; i < count; i++) {
            newProducers[i].producerWriteCounter.set(step * i);
        }*/
    }

    private void updateConsumers(AtomicBufferConsumer<T>[] newConsumers) {
        consumers = newConsumers;

        for (int i = 0; i < producers.length; i++) {
            producers[i].consumers = newConsumers;
        }

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
        private final Object[] data;

        private AtomicBufferConsumer<T>[] consumers;

        private AlignedLong readCounter;
        private AlignedLong writeCounter;

        private AtomicBufferProducer(AtomicBufferRouter<T> parent) {
            this.parent = parent;

            size = parent.size;
            mask = parent.mask;
            data = parent.data;
            consumers = parent.consumers;

            writeCounter = new AlignedLong();
            writeCounter.set(0);

            readCounter = new AlignedLong();
            readCounter.set(0);
        }

        @Override
        public boolean addToTail(T element) {
            long localReadIndex = readCounter.get();
            long localWriteIndex = writeCounter.get();

            int index;
            Object lock;
            for (;;) {
                if (localWriteIndex >= localReadIndex + size) {
                    localReadIndex = consumers[0].readCounter.get();

                    for (int i = 1; i < consumers.length; i++) {
                        long readIndexFromConsumer = consumers[i].readCounter.get();
                        if (localReadIndex > readIndexFromConsumer) {
                            localReadIndex = readIndexFromConsumer;
                        }
                    }

                    if (localWriteIndex >= localReadIndex + size) {
                        return false;
                    } else {
                        readCounter.set(localReadIndex);
                    }
                }

                index = ((int) (localWriteIndex & mask)) << 1;
                lock = data[index];

                if (lock == EMPTY && ArrayMemory.compareAndSwapObject(data, index, EMPTY, DATA)) {
                    ArrayMemory.setObject(data, index + 1, element);
                    MemoryFence.store();
                    writeCounter.set(localWriteIndex + 1);
                    return true;
                } else if (lock == PULLING) {
                    continue;
                }

                localWriteIndex++;
                writeCounter.set(localWriteIndex);
            }
        }
    }


    private static final class AtomicBufferConsumer<T> implements QueueHead<T> {

        private final AtomicBufferRouter<T> parent;

        private final int mask;
        private final Object[] data;

        private AtomicBufferProducer<T>[] producers;

        private AlignedLong readCounter;
        private AlignedLong writeCounter;

        private AtomicBufferConsumer(AtomicBufferRouter<T> parent) {
            this.parent = parent;

            mask = parent.mask;
            data = parent.data;
            producers = parent.producers;

            readCounter = new AlignedLong();
            readCounter.set(0);

            writeCounter = new AlignedLong();
            writeCounter.set(0);
        }

        @Override
        public T removeFromHead() {
            long localReadIndex = readCounter.get();

            int index;
            Object lock;
            for (;;) {
                index = ((int) (localReadIndex & mask)) << 1;
                lock = data[index];

                if (lock == PUSHING) {
                    continue;
                } else if (lock == DATA && ArrayMemory.compareAndSwapObject(data, index, DATA, EMPTY)) {
                    Object element = ArrayMemory.getObject(data, index + 1);
                    MemoryFence.load();
                    readCounter.set(localReadIndex + 1);
                    return (T) element;
                }

                localReadIndex++;
                readCounter.set(localReadIndex);
            }
        }
    }
}
