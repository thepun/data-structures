package org.thepun.data.queue;

public interface Router<T> {

    QueueTail<T> createProducer();

    void destroyProducer(QueueTail<T> producer);

    QueueHead<T> createConsumer();

    void destroyConsumer(QueueHead<T> consumer);

}
