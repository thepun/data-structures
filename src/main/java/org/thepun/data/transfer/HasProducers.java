package org.thepun.data.transfer;

public interface HasProducers<T> {

    QueueTail<T> createProducer();

    void destroyProducer(QueueTail<T> producer);

}
