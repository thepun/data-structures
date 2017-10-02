package org.thepun.data.transfer;

abstract class AbstractConsumer<T, P> implements QueueHead<T> {

    private final P parent;

    AbstractConsumer(P parent) {
        this.parent = parent;
    }

    final P getParent() {
        return parent;
    }
}
