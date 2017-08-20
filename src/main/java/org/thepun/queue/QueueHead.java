package org.thepun.queue;

/**
 * Created by thepun on 20.08.17.
 */
public interface QueueHead<T> {

    /**
     * Remove and return element from the head of the queue
     *
     * @return head element or null
     */
    T removeFromHead();

}
