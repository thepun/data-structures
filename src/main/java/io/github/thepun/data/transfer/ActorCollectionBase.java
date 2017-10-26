package io.github.thepun.data.transfer;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Arrays;

abstract class ActorCollectionBase<T, P extends AbstractProducer<T, ? extends ActorCollectionBase<T, ?, ?>>,
        C extends AbstractConsumer<T, ? extends ActorCollectionBase<T, ?, ?>>> implements HasProducers<T>, HasConsumers<T> {

    private final Class<P> producerType;
    private final Class<C> consumerType;
    private final Constructor<P> producerConstructor;
    private final Constructor<C> consumerConstructor;

    private P[] producers;
    private C[] consumers;

    ActorCollectionBase(Class<? extends AbstractProducer> producers, Class<? extends AbstractConsumer> consumers) {
        this.producerType = (Class<P>) producers;
        this.consumerType = (Class<C>) consumers;

        try {
            producerConstructor = producerType.getConstructor(getClass());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }

        try {
            consumerConstructor = consumerType.getConstructor(getClass());
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
        updateProducers(newProducers);
        return producer;
    }

    @Override
    public final QueueHead<T> createConsumer() {
        C[] oldConsumers = consumers;
        C[] newConsumers = Arrays.copyOf(oldConsumers, oldConsumers.length + 1);
        C consumer = createConsumerInstance();
        afterConsumerCreated(consumer);
        newConsumers[oldConsumers.length] = consumer;
        updateConsumers(newConsumers);
        return consumer;
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
        updateProducers(newProducers);

        beforeProducerDestroied(producerToDelete);
    }

    @Override
    public final void destroyConsumer(QueueHead<T> consumer) {
        if (!consumerType.isInstance(consumer)) {
            throw new IllegalArgumentException("Wrong consumer");
        }

        C consumerToDelete = (C) consumer;
        if (consumerToDelete.getParent() != this) {
            throw new IllegalArgumentException("Consumer from another router");
        }

        C[] newConsumers;
        C[] oldConsumers;

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

        newConsumers = (C[]) Array.newInstance(consumerType, oldConsumers.length - 1);
        System.arraycopy(oldConsumers, 0, newConsumers, 0, index);
        System.arraycopy(oldConsumers, index + 1, newConsumers, index + 1 - 1, oldConsumers.length - (index + 1));
        updateConsumers(newConsumers);

        beforeConsumerDestroied(consumerToDelete);
    }

    P createProducerInstance() {
        try {
            return producerConstructor.newInstance(this);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create producer instance", e);
        }
    }

    C createConsumerInstance() {
        try {
            return consumerConstructor.newInstance(this);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create producer instance", e);
        }
    }

    void afterProducerCreated(P producer) {
    }

    void afterConsumerCreated(C consumer) {
    }

    void beforeProducerDestroied(P producer) {
    }

    void beforeConsumerDestroied(C consumer) {
    }

    void syncConsumerWithNewProducers(C consumer, P[] producers) {
    }

    void syncProducerWithNewConsumers(P producer, C[] consumers) {
    }

    private void updateProducers(P[] newProducers) {
        producers = newProducers;

        for (int i = 0; i < consumers.length; i++) {
            syncConsumerWithNewProducers(consumers[i], newProducers);
        }
    }

    private void updateConsumers(C[] newConsumers) {
        consumers = newConsumers;

        for (int i = 0; i < producers.length; i++) {
            syncProducerWithNewConsumers(producers[i], newConsumers);
        }
    }
}
