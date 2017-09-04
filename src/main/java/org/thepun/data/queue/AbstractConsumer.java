package org.thepun.data.queue;

abstract class AbstractConsumer<T> implements QueueHead<T> {

    private final AbstractRouter<T> parent;

    protected AbstractConsumer(AbstractRouter<T> parent) {
        this.parent = parent;
    }

    void checkParent(AbstractRouter<T> anotherParent) {
        if (parent != anotherParent) {
            throw new IllegalArgumentException("Wrong consumer");
        }
    }
}
