package org.thepun.data.transfer;

import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by thepun on 30.08.17.
 */
final class QueueAdapter<T> implements QueueHead<T>, QueueTail<T> {

    private final Queue<T> queue;

    public QueueAdapter(Queue<T> queue) {
        this.queue = queue;
    }

    @Override
    public boolean addToTail(T element) {
        return queue.offer(element);
    }

    @Override
    public T removeFromHead() {
        return queue.poll();
    }

    @Override
    public T removeFromHead(long timeout, TimeUnit timeUnit) throws TimeoutException, InterruptedException {
        return null;
    }



}
