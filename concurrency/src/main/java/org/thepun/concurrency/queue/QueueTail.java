package org.thepun.concurrency.queue;

/**
 * Created by thepun on 20.08.17.
 */
public interface QueueTail<T> {

    boolean addToTail(T element);

}