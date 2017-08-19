package org.thepun.queue.spsc;

import org.thepun.queue.SimpleBlockingQueue;

import java.util.concurrent.TimeoutException;

/**
 * Created by thepun on 19.08.17.
 */
public class SPSCBlockingLinkedQueue<T> extends SPSCLinkedQueue<T> implements SimpleBlockingQueue<T> {
    @Override
    public T removeFromHead(long timeout) throws TimeoutException {
        return null;
    }
}
