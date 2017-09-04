package org.thepun.data.queue;

abstract class AbstractProducer<T> implements QueueTail<T> {

    private final AbstractRouter<T> parent;

    protected AbstractProducer(AbstractRouter<T> parent) {
        this.parent = parent;
    }

    void checkParent(AbstractRouter<T> anotherParent) {
        if (parent != anotherParent) {
            throw new IllegalArgumentException("Wrong producer");
        }
    }
}
