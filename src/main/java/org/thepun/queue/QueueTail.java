package org.thepun.queue;

/**
 * Created by thepun on 20.08.17.
 */
public interface QueueTail<T> {

    /**
     * Add element to the tail of the queue
     *
     * @param element
     */
    void addToTail(T element);
}
