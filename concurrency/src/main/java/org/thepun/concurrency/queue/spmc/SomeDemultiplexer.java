package org.thepun.concurrency.queue.spmc;

import org.thepun.concurrency.queue.QueueHead;
import org.thepun.concurrency.queue.Demultiplexer;

public class SomeDemultiplexer<T> implements Demultiplexer<T> {

    @Override
    public QueueHead<T> createConsumer() {
        return null;
    }

    @Override
    public void destroyConsumer(QueueHead<T> consumer) {

    }

    @Override
    public boolean addToTail(T element) {
        return false;
    }
}
