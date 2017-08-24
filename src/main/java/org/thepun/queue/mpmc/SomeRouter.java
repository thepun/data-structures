package org.thepun.queue.mpmc;

import org.thepun.queue.QueueHead;
import org.thepun.queue.QueueTail;
import org.thepun.queue.Router;

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
