package org.thepun.concurrency.queue.spsc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import org.thepun.concurrency.queue.QueueHead;
import org.thepun.concurrency.queue.BlockingQueueHead;
import org.thepun.concurrency.queue.QueueTail;

/**
 * Created by thepun on 19.08.17.
 */
public class BlockingLinkedArrayQueue<T> extends LinkedArrayQueue<T> implements BlockingQueueHead<T>, QueueHead<T>, QueueTail<T> {

    private final AtomicReference<Thread> consumerThread;

    public BlockingLinkedArrayQueue() {
        consumerThread = new AtomicReference<>();
    }

    @Override
    public void addToTail(T element) {
        super.addToTail(element);

        Thread waiter = consumerThread.get();
        LockSupport.unpark(waiter);
    }

    @Override
    public T removeFromBlockingHead(long timeout, TimeUnit timeUnit) throws TimeoutException, InterruptedException {
        long start = System.nanoTime();
        long time = timeUnit.toNanos(timeout);

        Thread thread = Thread.currentThread();
        consumerThread.set(thread);

        T element;
        for (;;) {
            element = super.removeFromHead();
            if (element != null) {
                consumerThread.lazySet(null);
                return element;
            }

            long left = System.nanoTime() - start;
            if (left < time) {
                LockSupport.parkNanos(left);
                if (Thread.interrupted()) {
                    consumerThread.lazySet(null);
                    throw new InterruptedException();
                }
            } else {
                consumerThread.lazySet(null);
                throw new TimeoutException();
            }
        }
    }
}
