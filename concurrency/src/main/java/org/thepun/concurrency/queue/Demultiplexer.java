package org.thepun.concurrency.queue;

public interface Demultiplexer<T> extends QueueTail<T> {

    QueueHead<T> createConsumer();

    void destroyConsumer(QueueHead<T> consumer);

}
