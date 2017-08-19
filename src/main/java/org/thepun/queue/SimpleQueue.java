package org.thepun.queue;

public interface SimpleQueue<T> {

    /**
     * Add element to the tail of the queue
     *
     * @param element
     */
    void addToTail(T element);

    /**
     * Remove and return element from the head of the queue
     *
     * @return head element or null
     */
    T removeFromHead();

}