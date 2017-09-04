package org.thepun.data.queue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.thepun.unsafe.ArrayMemory;
import org.thepun.unsafe.MemoryFence;

public final class RingBufferRouter<T> extends AbstractRouter<T> {

    private final int size;
    private final Object[] data;
    private final AlignedLong readCounter;
    private final AlignedLong writeCounter;


    public RingBufferRouter(int bufferSize) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("Size should be greater then zero");
        }

        size = bufferSize;
        data = new Object[bufferSize];
        readCounter = new AlignedLong();
        writeCounter = new AlignedLong();
    }

    @Override
    protected AbstractProducer<T>[] createProducerArray(int length) {
        return new RingBufferProducer[length];
    }

    @Override
    protected AbstractConsumer<T>[] createConsumerArray(int length) {
        return new RingBufferConsumer[length];
    }

    @Override
    protected RingBufferProducer<T> createProducerInstance() {
        return new RingBufferProducer<>(this);
    }

    @Override
    protected RingBufferConsumer<T> createConsumerInstance() {
        return new RingBufferConsumer<>(this);
    }

    @Override
    protected void afterProducerUpdate() {
        for (AbstractConsumer<T> consumer : consumers) {
            ((RingBufferConsumer<T>) consumer).producers = (RingBufferProducer<T>[]) producers;
        }
    }

    @Override
    protected void afterConsumerUpdate() {
        for (AbstractProducer<T> producer : producers) {
            ((RingBufferProducer<T>) producer).consumers = (RingBufferConsumer<T>[]) consumers;
        }
    }


    private static final class RingBufferProducer<T> extends AbstractProducer<T> {

        private final int size;
        private final Object[] data;
        private final AlignedLong readCounter;
        private final AlignedLong writeCounter;

        private long localReadCounter;
        private long localWriteCounter;

        private RingBufferConsumer<T>[] consumers;

        private RingBufferProducer(RingBufferRouter<T> parent) {
            super(parent);

            size = parent.size;
            data = parent.data;
            readCounter = parent.readCounter;
            writeCounter = parent.writeCounter;
            consumers = (RingBufferConsumer<T>[]) parent.consumers;
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


    private static final class RingBufferConsumer<T> extends AbstractConsumer<T> {

        private final int size;
        private final Object[] data;
        private final AlignedLong readCounter;
        private final AlignedLong writeCounter;

        private long localReadCounter;
        private long localWriteCounter;

        private RingBufferProducer<T>[] producers;

        private RingBufferConsumer(RingBufferRouter<T> parent) {
            super(parent);

            size = parent.size;
            data = parent.data;
            readCounter = parent.readCounter;
            writeCounter = parent.writeCounter;
            producers = (RingBufferProducer<T>[]) parent.producers;
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

        @Override
        public T removeFromHead(long timeout, TimeUnit timeUnit) throws TimeoutException, InterruptedException {
            //TODO: implement busy wait
            return null;
        }
    }
}
