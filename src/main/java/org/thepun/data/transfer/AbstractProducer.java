package org.thepun.data.transfer;

abstract class AbstractProducer<T, P> implements QueueTail<T> {

    private final P parent;

    AbstractProducer(P parent) {
        this.parent = parent;
    }

    final P getParent() {
        return parent;
    }
}
