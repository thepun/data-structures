package org.thepun.data.queue;

import java.util.Arrays;

import org.thepun.unsafe.ArrayMemory;

public final class AtomicBufferRouter<T> implements Router<T> {

    // TODO: align producers/consumers
    // TODO: get rid of size


    private final int size;
    private final int mask;
    private final Object[] data;
    private final Generation[] generations;

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
        consumers = new AtomicBufferConsumer[0];
        producers = new AtomicBufferProducer[0];

        generations = new Generation[10];
        for (int i = 0; i < 10; i++) {
            generations[i] = new Generation(i);
        }

        for (int i = 0; i < data.length; i++) {
            data[i] = generations[1];
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
        private final Generation[] generations;

        private int generationIndex;
        private Generation currentGeneration;
        private Generation previousGeneration;
        private AtomicBufferConsumer<T>[] consumers;
        private AlignedLong producerReadCounter;
        private AlignedLong producerWriteCounter;

        private AtomicBufferProducer(AtomicBufferRouter<T> parent) {
            this.parent = parent;

            size = parent.size;
            mask = parent.mask;
            data = parent.data;
            consumers = parent.consumers;
            generations = parent.generations;

            producerReadCounter = new AlignedLong();
            producerReadCounter.set(0);

            producerWriteCounter = new AlignedLong();
            producerWriteCounter.set(0);

            generationIndex = 1;
            currentGeneration = generations[1];
            previousGeneration = generations[0];
        }

        @Override
        public boolean addToTail(T element) {
            long localReadCounter = producerReadCounter.get();
            long localWriteCounter = producerWriteCounter.get();

            if (localWriteCounter >= localReadCounter + size) {
                localReadCounter = consumers[0].consumerReadCounter.get();
                for (int i = 1; i < consumers.length; i++) {
                    long localReadCounterFromConsumer = consumers[i].consumerReadCounter.get();
                    if (localReadCounter > localReadCounterFromConsumer) {
                        localReadCounter = localReadCounterFromConsumer;
                    }
                }
                producerReadCounter.set(localReadCounter);
            }

            int index;
            Object currentElement;
            while (localWriteCounter < localReadCounter + size) {
                if (localWriteCounter % size == 0) {
                    previousGeneration = currentGeneration;
                    currentGeneration = generations[++generationIndex % 10];
                }

                index = (int) (localWriteCounter & mask);

                currentElement = data[index];
                if (currentElement == previousGeneration) {
                    if (ArrayMemory.compareAndSwapObject(data, index, previousGeneration, element)) {
                        producerWriteCounter.set(localWriteCounter + 1);
                        return true;
                    }
                    localWriteCounter++;
                } else if (currentElement == currentGeneration) {
                    localWriteCounter++;
                } else {
                    return false;
                }
            }

            return false;
        }
    }


    private static final class AtomicBufferConsumer<T> implements QueueHead<T> {

        private final AtomicBufferRouter<T> parent;

        private final int size;
        private final int mask;
        private final Object[] data;
        private final Generation[] generations;

        private int generationIndex;
        private Generation currentGeneration;
        private Generation previousGeneration;
        private AlignedLong consumerReadCounter;

        private AtomicBufferConsumer(AtomicBufferRouter<T> parent) {
            this.parent = parent;

            size = parent.size;
            mask = parent.mask;
            data = parent.data;
            generations = parent.generations;

            consumerReadCounter = new AlignedLong();
            consumerReadCounter.set(0);

            generationIndex = 1;
            currentGeneration = generations[1];
            previousGeneration = generations[0];
        }

        @Override
        public T removeFromHead() {
            long localReadCounter = consumerReadCounter.get();

            int index;
            Object element;
            for (;;) {
                if (localReadCounter % size == 0) {
                    previousGeneration = currentGeneration;
                    currentGeneration = generations[++generationIndex % 10];
                }

                index = (int) (localReadCounter & mask);

                element = data[index];
                if (element == previousGeneration) {
                    return null;
                } else if (element == currentGeneration) {
                    localReadCounter++;
                    consumerReadCounter.set(localReadCounter);
                } else if (ArrayMemory.compareAndSwapObject(data, index, element, currentGeneration)) {
                    consumerReadCounter.set(localReadCounter + 1);
                    return (T) element;
                } else {
                    localReadCounter++;
                    consumerReadCounter.set(localReadCounter);
                }
            }
        }
    }
}
