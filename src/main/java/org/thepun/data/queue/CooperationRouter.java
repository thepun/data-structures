package org.thepun.data.queue;

public class CooperationRouter<T> implements Router<T> {
    @Override
    public QueueTail<T> createProducer() {
        return null;
    }

    @Override
    public void destroyProducer(QueueTail<T> producer) {

    }

    @Override
    public QueueHead<T> createConsumer() {
        return null;
    }

    @Override
    public void destroyConsumer(QueueHead<T> consumer) {

    }
}
