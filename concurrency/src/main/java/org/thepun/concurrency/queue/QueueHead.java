package org.thepun.concurrency.queue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Consumer's entry point interface.
 *
 * Provides very basic functionality to pop elements from a queue. Generally there is no limitations on whether an instance of the interface should be used
 * from a strictly particular thread but usually implementation details apply such limitations.
 */
public interface QueueHead<T> {

    /**
     * Remove and return element from the head of the queue
     *
     * @return head element or null
     */
    T removeFromHead();

    /**
     * Remove and return element from the head of the queue.
     * If there is now element then wait until it comes.
     *
     * @param timeout - maximum amount of time to wait until throw exception
     * @return element
     * @throws TimeoutException
     */
    T removeFromHead(long timeout, TimeUnit timeUnit) throws TimeoutException, InterruptedException;

}
