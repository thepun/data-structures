package org.thepun.queue;

public interface Multiplexer<T> extends QueueHead<T> {

    /**
     * Created producer entry point. Should be executed once per each producer thread and used from that thread.
     *
     * Implementation of the result object in common case is not thread-safe.
     *
     * @return subqueue for single producer
     */
    QueueTail<T> createProducer();

    /**
     * Destroys producer entry point.
     *
     * @param producer which is not required anymore
     */
    void destroyProducer(QueueTail<T> producer);

}
