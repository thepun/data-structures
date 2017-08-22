package org.thepun.queue.spmc;

import org.thepun.queue.Demultiplexer;
import org.thepun.queue.QueueHead;

public class SPMCDemultiplexer<T> implements Demultiplexer<T> {

    @Override
    public QueueHead<T> createConsumer() {
        return null;
    }

    @Override
    public void destroyConsumer(QueueHead<T> consumer) {

    }

    @Override
    public void addToTail(T element) {

    }
}
