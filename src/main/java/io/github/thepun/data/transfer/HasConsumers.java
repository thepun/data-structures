package io.github.thepun.data.transfer;

public interface HasConsumers<T> {

    QueueHead<T> createConsumer();

    void destroyConsumer(QueueHead<T> consumer);

}
