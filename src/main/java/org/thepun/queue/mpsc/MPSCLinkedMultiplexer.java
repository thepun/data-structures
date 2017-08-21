package org.thepun.queue.mpsc;

import org.thepun.queue.QueueHead;
import org.thepun.queue.QueueTail;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by thepun on 19.08.17.
 */
@SuppressWarnings("unchecked")
public class MPSCLinkedMultiplexer<T> implements QueueHead<T> {

    private static final int BUNCH_SIZE = 256;
    private static final int FIRST_ITEM_INDEX = 1;
    private static final int SECOND_ITEM_INDEX = 2;
    private static final int REF_TO_NEXT_INDEX = 0;
    private static final int FIRST_OFFSET_INDEX = BUNCH_SIZE;
    private static final Object[] EMPTY_ARRAY = new Object[BUNCH_SIZE];


    private int nextProducerIndex;
    private volatile ProducerSubqueue[] producers;

    public MPSCLinkedMultiplexer() {
        producers = new ProducerSubqueue[0];
        nextProducerIndex = 0;
    }

    public synchronized QueueTail<T> createProducer() {
        ProducerSubqueue<T> producer = new ProducerSubqueue<>();
        int length = producers.length;
        producers = Arrays.copyOf(producers, length + 1);
        producers[length] = producer;
        return producer;
    }

    @Override
    public T removeFromHead() {
        ProducerSubqueue[] localProducers = producers;

        int producerIndex = nextProducerIndex;
        int producerCount = localProducers.length;
        int maxProducerIndex = producerIndex + producerCount;
        for (;;) {
            if (producerIndex >= maxProducerIndex) {
                nextProducerIndex = 0;
                return null;
            }

            ProducerSubqueue<T> producer = producers[producerIndex % producerCount];

            if (producer.consumerIndex == FIRST_OFFSET_INDEX) {
                Object[] oldConsumerBunch = producer.consumerBunch;

                Object[] newHeadBunch = (Object[]) oldConsumerBunch[REF_TO_NEXT_INDEX];
                if (newHeadBunch == null) {
                    continue;
                }

                producer.consumerIndex = FIRST_ITEM_INDEX;
                producer.consumerBunch = newHeadBunch;

                System.arraycopy(EMPTY_ARRAY, 0, oldConsumerBunch, 0, BUNCH_SIZE);

                Object[] prevEmptyChainHead = producer.emptyChain.get();
                if (prevEmptyChainHead == null) {
                    producer.emptyChain.lazySet(oldConsumerBunch);
                } else {
                    oldConsumerBunch[REF_TO_NEXT_INDEX] = prevEmptyChainHead;
                    if (!producer.emptyChain.compareAndSet(prevEmptyChainHead, oldConsumerBunch)) {
                        oldConsumerBunch[REF_TO_NEXT_INDEX] = null;
                        producer.emptyChain.lazySet(oldConsumerBunch);
                    }
                }
            }

            Object element = producer.consumerBunch[producer.consumerIndex];
            if (element != null) {
                nextProducerIndex = producerIndex;
                producer.consumerIndex++;
                return (T) element;
            }

            producerIndex++;
        }
    }


    private static class ProducerSubqueue<T> implements QueueTail<T> {

        private int consumerIndex;
        private Object[] consumerBunch;

        private int producerIndex;
        private Object[] producerBunch;
        private Object[] producerEmptyChain;

        private final AtomicReference<Object[]> emptyChain;

        private ProducerSubqueue() {
            Object[] firstBunch = new Object[BUNCH_SIZE];
            consumerBunch = firstBunch;
            producerBunch = firstBunch;
            consumerIndex = FIRST_ITEM_INDEX;
            producerIndex = FIRST_ITEM_INDEX;

            emptyChain = new AtomicReference<>();
        }

        @Override
        public void addToTail(T element) {
            if (producerIndex == FIRST_OFFSET_INDEX) {
                Object[] newTailBunch;

                if (producerEmptyChain == null) {
                    Object[] newChain = emptyChain.getAndSet(null);
                    if (newChain == null) {
                        newChain = new Object[BUNCH_SIZE];
                    }

                    producerEmptyChain = newChain;
                }

                newTailBunch = producerEmptyChain;
                producerEmptyChain = (Object[]) producerEmptyChain[REF_TO_NEXT_INDEX];
                newTailBunch[REF_TO_NEXT_INDEX] = null;
                newTailBunch[FIRST_ITEM_INDEX] = element;
                producerBunch[REF_TO_NEXT_INDEX] = newTailBunch;
                producerBunch = newTailBunch;
                producerIndex = SECOND_ITEM_INDEX;

                return;
            }

            producerBunch[producerIndex++] = element;
        }
    }
}
