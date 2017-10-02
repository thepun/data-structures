package org.thepun.data.transfer;

final class CooperationConsumer<T> extends AbstractConsumer<T, CooperationRouter<T>> {

    CooperationConsumer() {
        super(parent);
    }

    @Override
    public T removeFromHead() {
        return null;
    }
}
