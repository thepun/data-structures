package org.thepun.data.transfer;

public final class CooperationRouter<T> extends ActorCollectionBase<T, CooperationProducer<T>, CooperationConsumer<T>> {

    public CooperationRouter() {
        super(CooperationProducer.class, CooperationConsumer.class);
    }


}
