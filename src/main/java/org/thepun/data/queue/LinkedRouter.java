package org.thepun.data.queue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LinkedRouter<T> extends AbstractRouter<T> {


    @Override
    protected AbstractProducer<T>[] createProducerArray(int length) {
        return new AbstractProducer[0];
    }

    @Override
    protected AbstractConsumer<T>[] createConsumerArray(int length) {
        return new AbstractConsumer[0];
    }

    @Override
    protected AbstractProducer<T> createProducerInstance() {
        return null;
    }

    @Override
    protected AbstractConsumer<T> createConsumerInstance() {
        return null;
    }

    @Override
    protected void afterProducerUpdate() {

    }

    @Override
    protected void afterConsumerUpdate() {

    }


    private static final class LinkedConsumer<T> extends AbstractConsumer<T> {

        private LinkedConsumer(AbstractRouter<T> parent) {
            super(parent);
        }

        @Override
        public T removeFromHead() {
            return null;
        }

        @Override
        public T removeFromHead(long timeout, TimeUnit timeUnit) throws TimeoutException, InterruptedException {
            return null;
        }
    }


    private static final class LinkedProducer<T> extends AbstractProducer<T> {

        private LinkedProducer(AbstractRouter<T> parent) {
            super(parent);
        }

        @Override
        public boolean addToTail(T element) {
            return false;
        }
    }
}
