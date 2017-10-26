package io.github.thepun.data.transfer;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Arrays;

abstract class ConsumerCollectionBase<T, C extends AbstractConsumer<T, ? extends ConsumerCollectionBase<T, ?>>> implements HasConsumers<T> {

    private final Class<C> consumerType;
    private final Constructor<C> consumerConstructor;

    private C[] consumers;

    ConsumerCollectionBase(Class<? extends AbstractConsumer> consumers) {
        this.consumerType = (Class<C>) consumers;

        try {
            consumerConstructor = consumerType.getConstructor(getClass());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public final QueueHead<T> createConsumer() {
        C[] oldConsumers = consumers;
        C[] newConsumers = Arrays.copyOf(oldConsumers, oldConsumers.length + 1);
        C consumer = createConsumerInstance();
        afterConsumerCreated(consumer);
        newConsumers[oldConsumers.length] = consumer;
        consumers = newConsumers;
        return consumer;
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
        consumers = newConsumers;

        beforeConsumerDestroied(consumerToDelete);
    }

    C createConsumerInstance() {
        try {
            return consumerConstructor.newInstance(this);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create producer instance", e);
        }
    }

    void afterConsumerCreated(C consumer) {
    }

    void beforeConsumerDestroied(C consumer) {
    }
}
