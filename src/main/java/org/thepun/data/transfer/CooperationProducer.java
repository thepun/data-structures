package org.thepun.data.transfer;

final class CooperationProducer<T> extends AbstractProducer<T, CooperationRouter<T>> {

    CooperationProducer(CooperationRouter<T> parent) {
        super(parent);
    }

    @Override
    public boolean addToTail(T element) {
        return false;
    }
}
