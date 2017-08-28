package org.thepun.concurrency.queue.spmc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.thepun.concurrency.queue.QueueHead;
import org.thepun.concurrency.queue.Demultiplexer;

public class RoundRobinDemultiplexer<T> implements Demultiplexer<T> {

    private int consumerIndex;
    private ConsumerSubqueue<T>[] consumers;

    @Override
    public synchronized QueueHead<T> createConsumer() {
        return null;
    }

    @Override
    public synchronized void destroyConsumer(QueueHead<T> consumer) {

    }

    @Override
    public boolean addToTail(T element) {
        return false;
    }


    private static final class ConsumerSubqueue<T> implements QueueHead<T> {

        @Override
        public T removeFromHead() {
            return null;
        }

        @Override
        public T removeFromHead(long timeout, TimeUnit timeUnit) throws TimeoutException, InterruptedException {
            return null;
        }
    }
}
