package org.thepun.queue.spsc;

import org.thepun.queue.SimpleBlockingQueue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by thepun on 19.08.17.
 */
public class BlockingLinkedArrayQueue<T> extends LinkedArrayQueue<T> implements SimpleBlockingQueue<T> {

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
    public T removeFromHead(long timeout, TimeUnit timeUnit) throws TimeoutException, InterruptedException {
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
