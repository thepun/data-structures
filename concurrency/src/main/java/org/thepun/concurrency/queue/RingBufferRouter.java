package org.thepun.concurrency.queue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.thepun.unsafe.ArrayMemory;
import org.thepun.unsafe.Fence;

public final class RingBufferRouter<T> implements Router<T> {

    private final int size;
    private final Object[] data;
    private final AlignedLong readCounter;
    private final AlignedLong writeCounter;
    private final AlignedLong readCounterForProducers;
    private final AlignedLong writeCounterForConsumers;

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
        readCounterForProducers = new AlignedLong();
        writeCounterForConsumers = new AlignedLong();

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
        Fence.full();
        producers = newProducers;
        for (int i = 0; i < newProducers.length; i++) {
            newProducers[i].producers = newProducers;
        }
        Fence.full();
    }

    private void updateConsumers(RingBufferConsumer<T>[] newConsumers) {
        Fence.full();
        consumers = newConsumers;
        for (int i = 0; i < newConsumers.length; i++) {
            newConsumers[i].consumers = newConsumers;
        }
        Fence.full();
    }


    private static final class RingBufferProducer<T> implements QueueTail<T> {

        private final RingBufferRouter<T> parent;

        private final int size;
        private final Object[] data;
        private final AlignedLong writeCounter;
        private final AlignedLong readCounterForProducers;
        private final AlignedLong writeCounterForConsumers;

        private long localWriteCounterForConsumers;
        private RingBufferProducer<T>[] producers;

        private RingBufferProducer(RingBufferRouter<T> parent) {
            this.parent = parent;

            size = parent.size;
            data = parent.data;
            writeCounter = parent.writeCounter;
            readCounterForProducers = parent.readCounterForProducers;
            writeCounterForConsumers = parent.writeCounterForConsumers;
        }

        @Override
        public boolean addToTail(T element) {
            long readIndex = readCounterForProducers.get();
            long writeIndex = writeCounter.getAndIncrement(readIndex + size);
            if (writeIndex == -1) {
                return false;
            }

            int index = (int) writeIndex % size;
            ArrayMemory.setObject(data, index, element);
            Fence.store();

            long minWriteCounterForConsumers = writeIndex + 1;
            localWriteCounterForConsumers = minWriteCounterForConsumers;
            for (int i = 0; i < producers.length; i++) {
                long anotherLocalCounter = producers[i].localWriteCounterForConsumers;
                if (minWriteCounterForConsumers > anotherLocalCounter) {
                    minWriteCounterForConsumers = anotherLocalCounter;
                }
            }
            writeCounterForConsumers.set(minWriteCounterForConsumers);

            return true;
        }
    }


    private static final class RingBufferConsumer<T> implements QueueHead<T> {

        private final RingBufferRouter<T> parent;

        private final int size;
        private final Object[] data;
        private final AlignedLong readCounter;
        private final AlignedLong readCounterForProducers;
        private final AlignedLong writeCounterForConsumers;

        private long localReadCounterForProducers;
        private RingBufferConsumer<T>[] consumers;

        private RingBufferConsumer(RingBufferRouter<T> parent) {
            this.parent = parent;

            size = parent.size;
            data = parent.data;
            readCounter = parent.readCounter;
            readCounterForProducers = parent.readCounterForProducers;
            writeCounterForConsumers = parent.writeCounterForConsumers;
        }

        @Override
        public T removeFromHead() {
            long writeIndex = writeCounterForConsumers.get();
            long readIndex = readCounter.getAndIncrement(writeIndex);
            if (readIndex == -1) {
                return null;
            }

            int index = (int) readIndex % size;
            Object element;
            do {
                element = ArrayMemory.getObject(data, index);
            } while (element == null);
            ArrayMemory.setObject(data, index, null);
            Fence.store();

            long minReadCounterForProducers = readIndex + 1;
            localReadCounterForProducers = minReadCounterForProducers;
            for (int i = 0; i < consumers.length; i++) {
                long anotherLocalCounter = consumers[i].localReadCounterForProducers;
                if (minReadCounterForProducers > anotherLocalCounter) {
                    minReadCounterForProducers = anotherLocalCounter;
                }
            }
            readCounterForProducers.set(minReadCounterForProducers);

            return (T) element;
        }

        @Override
        public T removeFromHead(long timeout, TimeUnit timeUnit) throws TimeoutException, InterruptedException {
            //TODO: implement busy wait
            return null;
        }
    }
}
