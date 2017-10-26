package io.github.thepun.data.transfer;

final class CooperationConsumer<T> extends AbstractConsumer<T, CooperationRouter<T>> {

    CooperationConsumer(CooperationRouter<T> parent) {
        super(parent);
    }

    @Override
    public T removeFromHead() {
        return null;
    }
}
