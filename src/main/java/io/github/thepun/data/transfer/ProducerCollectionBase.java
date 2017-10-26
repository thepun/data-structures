package io.github.thepun.data.transfer;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Arrays;

abstract class ProducerCollectionBase<T, P extends AbstractProducer<T, ? extends ProducerCollectionBase<T, ?>>> implements HasProducers<T> {

    private final Class<P> producerType;
    private final Constructor<P> producerConstructor;

    private P[] producers;

    ProducerCollectionBase(Class<? extends AbstractProducer> producers) {
        this.producerType = (Class<P>) producers;

        try {
            producerConstructor = producerType.getConstructor(getClass());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public final synchronized QueueTail<T> createProducer() {
        P[] oldProducers = producers;
        P[] newProducers = Arrays.copyOf(oldProducers, oldProducers.length + 1);
        P producer = createProducerInstance();
        afterProducerCreated(producer);
        newProducers[oldProducers.length] = producer;
        producers = newProducers;
        return producer;
    }

    @Override
    public final synchronized void destroyProducer(QueueTail<T> producer) {
        if (!producerType.isInstance(producer)) {
            throw new IllegalArgumentException("Wrong producer");
        }

        P producerToDelete = (P) producer;
        if (producerToDelete.getParent() != this) {
            throw new IllegalArgumentException("Producer from another router");
        }

        P[] newProducers;
        P[] oldProducers;

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

        newProducers = (P[]) Array.newInstance(producerType, oldProducers.length - 1);
        System.arraycopy(oldProducers, 0, newProducers, 0, index);
        System.arraycopy(oldProducers, index + 1, newProducers, index + 1 - 1, oldProducers.length - (index + 1));
        producers = newProducers;

        beforeProducerDestroied(producerToDelete);
    }

    P createProducerInstance() {
        try {
            return producerConstructor.newInstance(this);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create producer instance", e);
        }
    }

    void afterProducerCreated(P producer) {
    }

    void beforeProducerDestroied(P producer) {
    }
}
