package org.thepun.concurrency.queue;

import static org.thepun.concurrency.queue.QueueConstants.LINKED_BUNCH_SIZE;
import static org.thepun.concurrency.queue.QueueConstants.LINKED_FIRST_ITEM_INDEX;
import static org.thepun.concurrency.queue.QueueConstants.LINKED_FIRST_ITEM_INDEX_ADDRESS;
import static org.thepun.concurrency.queue.QueueConstants.LINKED_FIRST_OFFSET_INDEX;
import static org.thepun.concurrency.queue.QueueConstants.LINKED_NULLS_BUNCH;
import static org.thepun.concurrency.queue.QueueConstants.LINKED_REF_TO_NEXT_INDEX_ADDRESS;
import static org.thepun.concurrency.queue.QueueConstants.LINKED_SECOND_ITEM_INDEX;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.thepun.unsafe.ArrayMemory;

public final class RoundRobinLinkedDemultiplexer<T> implements Demultiplexer<T> {

    private int nextConsumerIndex;
    private ConsumerSubqueue<T>[] consumers;

    public RoundRobinLinkedDemultiplexer() {
        consumers = new ConsumerSubqueue[0];
    }

    @Override
    public synchronized QueueHead<T> createConsumer() {
        ConsumerSubqueue<T>[] oldConsumers = consumers;
        ConsumerSubqueue<T>[] newConsumers = Arrays.copyOf(oldConsumers, oldConsumers.length + 1);
        ConsumerSubqueue<T> consumer = new ConsumerSubqueue<>(this);
        newConsumers[oldConsumers.length] = consumer;
        consumers = newConsumers;
        return consumer;
    }

    @Override
    public synchronized void destroyConsumer(QueueHead<T> consumer) {
        if (!(consumer instanceof ConsumerSubqueue)) {
            throw new IllegalArgumentException("Wrong consumer");
        }

        ConsumerSubqueue<T> producerSubqueue = (ConsumerSubqueue<T>) consumer;
        if (producerSubqueue.parent != this) {
            throw new IllegalArgumentException("Consumer from another demultiplexer");
        }

        ConsumerSubqueue<T>[] newConsumers;
        ConsumerSubqueue<T>[] oldConsumers;

        oldConsumers = consumers;
        int index = -1;
        for (int i = 0; i < oldConsumers.length; i++) {
            if (oldConsumers[i] == consumer) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            throw new IllegalArgumentException("COnsumer not found");
        }

        newConsumers = new ConsumerSubqueue[oldConsumers.length - 1];
        System.arraycopy(oldConsumers, 0, newConsumers, 0, index);
        System.arraycopy(oldConsumers, index + 1, newConsumers, index + 1 - 1, oldConsumers.length - (index + 1));

        consumers = newConsumers;
    }

    @Override
    public boolean addToTail(T element) {
        ConsumerSubqueue<T>[] localConsumers = consumers;

        int consumerCount = localConsumers.length;
        if (consumerCount == 0) {
            return false;
        }

        int consumerIndex = (nextConsumerIndex + 1) % consumerCount;
        nextConsumerIndex = consumerIndex;

        ConsumerSubqueue<T> consumer = localConsumers[consumerIndex];
        AlignedLinkedNode tail = consumer.tail;
        int localIndex = tail.index;
        Object[] localBunch = tail.bunch;
        if (localIndex == LINKED_FIRST_OFFSET_INDEX) {
            Object[] localEmptyChain = tail.emptyChain;
            if (localEmptyChain == null) {
                Object[] newChain = consumer.emptyChain.getAndSet(null);
                if (newChain == null) {
                    newChain = new Object[LINKED_BUNCH_SIZE];
                }

                localEmptyChain = newChain;
            }

            ArrayMemory.setObject(localEmptyChain, LINKED_FIRST_ITEM_INDEX_ADDRESS, element);

            tail.emptyChain = (Object[]) ArrayMemory.getObject(localEmptyChain, LINKED_REF_TO_NEXT_INDEX_ADDRESS);
            ArrayMemory.setObject(localEmptyChain, LINKED_REF_TO_NEXT_INDEX_ADDRESS, null);
            ArrayMemory.setObject(localBunch, LINKED_REF_TO_NEXT_INDEX_ADDRESS, localEmptyChain);
            tail.bunch = localEmptyChain;
            tail.index = LINKED_SECOND_ITEM_INDEX;
            return true;
        }

        ArrayMemory.setObject(localBunch, localIndex, element);
        tail.index = localIndex + 1;
        return true;
    }


    private static final class ConsumerSubqueue<T> implements QueueHead<T> {

        private final AlignedLinkedNode head;
        private final AlignedLinkedNode tail;
        private final AtomicReference<Object[]> emptyChain;
        private final RoundRobinLinkedDemultiplexer<T> parent;

        private ConsumerSubqueue(RoundRobinLinkedDemultiplexer<T> parent) {
            this.parent = parent;

            Object[] firstBunch = new Object[LINKED_BUNCH_SIZE];
            head = new AlignedLinkedNode();
            tail = new AlignedLinkedNode();
            head.bunch = firstBunch;
            tail.bunch = firstBunch;
            head.index = LINKED_FIRST_ITEM_INDEX;
            tail.index = LINKED_FIRST_ITEM_INDEX;
            emptyChain = new AtomicReference<>();
        }

        @Override
        public T removeFromHead() {
            int localIndex = head.index;
            Object[] localBunch = head.bunch;
            if (localIndex == LINKED_FIRST_OFFSET_INDEX) {
                Object[] oldHeadBunh = localBunch;
                localBunch = (Object[]) ArrayMemory.getObject(localBunch, LINKED_REF_TO_NEXT_INDEX_ADDRESS);
                if (localBunch == null) {
                    // no more bunches at the moment
                    return null;
                }

                // change current bunch to the next one
                head.bunch = localBunch;
                head.index = LINKED_FIRST_ITEM_INDEX;
                localIndex = LINKED_FIRST_ITEM_INDEX;

                // clear array from reader thread to be sure about initial state without fences
                System.arraycopy(LINKED_NULLS_BUNCH, 0, oldHeadBunh, 0, LINKED_BUNCH_SIZE);

                // check if writer took all freed bunches
                Object[] prevEmptyChainHead = emptyChain.get();
                if (prevEmptyChainHead == null) {
                    // we need to cross fence to be able to rely on it
                    emptyChain.set(oldHeadBunh);
                } else {
                    // add empty bunch to list
                    ArrayMemory.setObject(oldHeadBunh, LINKED_REF_TO_NEXT_INDEX_ADDRESS, prevEmptyChainHead);

                    // if writer took empty bunches
                    if (!emptyChain.compareAndSet(prevEmptyChainHead, oldHeadBunh)) {
                        // ensure initial state is written by reader thread
                        ArrayMemory.setObject(oldHeadBunh, LINKED_REF_TO_NEXT_INDEX_ADDRESS, null);
                        // again we need to cross fence to be able to rely on it
                        emptyChain.set(oldHeadBunh);
                    }
                }
            }

            Object element = ArrayMemory.getObject(localBunch, localIndex);
            if (element != null) {
                head.index = localIndex + 1;
            }

            return (T) element;
        }

        @Override
        public T removeFromHead(long timeout, TimeUnit timeUnit) throws TimeoutException, InterruptedException {
            // TODO: implement busy wait
            return null;
        }
    }
}
