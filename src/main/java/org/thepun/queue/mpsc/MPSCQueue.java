package org.thepun.queue.mpsc;

import org.thepun.queue.SimpleQueue;

/**
 * Created by thepun on 19.08.17.
 */
public class MPSCQueue<T> implements SimpleQueue<T> {
    @Override
    public void addToTail(T element) {

    }

    @Override
    public T removeFromHead() {
        return null;
    }
}
