package org.thepun.concurrency.queue.mpmc;

import org.thepun.concurrency.queue.QueueHead;
import org.thepun.concurrency.queue.QueueTail;
import org.thepun.concurrency.queue.Router;

public class SomeRouter<T> implements Router<T> {

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
