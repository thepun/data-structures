package org.thepun.concurrency.queue;

public interface LimitedQueueTail<T> {

    boolean addToLimitedHead(T element);

}
