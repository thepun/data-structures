package org.thepun.queue.mpsc;

import org.thepun.queue.SimpleNonBlockingQueue;

/**
 * Created by thepun on 19.08.17.
 */
public class MPSCQueue<T> implements SimpleNonBlockingQueue<T> {
    @Override
    public void addToTail(T element) {

    }

    @Override
    public T removeFromHead() {
        return null;
    }
}
