package org.thepun.concurrency.queue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by thepun on 20.08.17.
 */
public interface BlockingQueueHead<T> {

    /**
     * Remove and return element from the head of the queue.
     * If there is now element then wait until it comes.
     *
     * @param timeout - maximum amount of time to wait until throw exception
     * @return element
     * @throws TimeoutException
     */
    T removeFromBlockingHead(long timeout, TimeUnit timeUnit) throws TimeoutException, InterruptedException;

}
