package org.thepun.data.queue;

import java.util.Arrays;

import org.thepun.unsafe.MemoryFence;

abstract class AbstractRouter<T> implements Router<T> {

    protected AbstractConsumer<T>[] consumers;
    protected AbstractProducer<T>[] producers;
    
    protected AbstractRouter() {
        consumers = createConsumerArray(0);
        producers = createProducerArray(0);
    }

    @Override
    public synchronized QueueTail<T> createProducer() {
        AbstractProducer<T>[] oldProducers = producers;
        AbstractProducer<T>[] newProducers = Arrays.copyOf(oldProducers, oldProducers.length + 1);
        AbstractProducer<T> producer = createProducerInstance();
        newProducers[oldProducers.length] = producer;
        afterProducerUpdate();
        return producer;
    }

    @Override
    public synchronized QueueHead<T> createConsumer() {
        AbstractConsumer<T>[] oldConsumers = consumers;
        AbstractConsumer<T>[] newConsumers = Arrays.copyOf(oldConsumers, oldConsumers.length + 1);
        AbstractConsumer<T> consumer = createConsumerInstance();
        newConsumers[oldConsumers.length] = consumer;
        afterConsumerUpdate();
        return consumer;
    }

    @Override
    public synchronized void destroyProducer(QueueTail<T> producer) {
        if (!(producer instanceof AbstractProducer)) {
            throw new IllegalArgumentException("Wrong producer");
        }

        AbstractProducer<T> producerSubqueue = (AbstractProducer<T>) producer;
        producerSubqueue.checkParent(this);

        AbstractProducer<T>[] newProducers;
        AbstractProducer<T>[] oldProducers;

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


        newProducers = createProducerArray(oldProducers.length - 1);
        System.arraycopy(oldProducers, 0, newProducers, 0, index);
        System.arraycopy(oldProducers, index + 1, newProducers, index + 1 - 1, oldProducers.length - (index + 1));
        afterProducerUpdate();
    }

    @Override
    public synchronized void destroyConsumer(QueueHead<T> consumer) {
        if (!(consumer instanceof AbstractConsumer)) {
            throw new IllegalArgumentException("Wrong consumer");
        }

        AbstractConsumer<T> producerSubqueue = (AbstractConsumer<T>) consumer;
        producerSubqueue.checkParent(this);

        AbstractConsumer<T>[] newConsumers;
        AbstractConsumer<T>[] oldConsumers;

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

        MemoryFence.full();
        newConsumers = createConsumerArray(oldConsumers.length - 1);
        System.arraycopy(oldConsumers, 0, newConsumers, 0, index);
        System.arraycopy(oldConsumers, index + 1, newConsumers, index + 1 - 1, oldConsumers.length - (index + 1));
        afterConsumerUpdate();
        MemoryFence.full();
    }

    protected abstract AbstractProducer<T>[] createProducerArray(int length);
    protected abstract AbstractConsumer<T>[] createConsumerArray(int length);
    protected abstract AbstractProducer<T> createProducerInstance();
    protected abstract AbstractConsumer<T> createConsumerInstance();
    protected abstract void afterProducerUpdate();
    protected abstract void afterConsumerUpdate();
    
}
