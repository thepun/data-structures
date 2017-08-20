package org.thepun.queue.mpsc;

import org.thepun.queue.QueueHead;
import org.thepun.queue.QueueTail;

import java.util.Arrays;

/**
 * Created by thepun on 19.08.17.
 */
public class MPSCLinkedMultiplexer<T> implements QueueHead<T> {

    private static final int BUNCH_SIZE = 256;
    private static final int FIRST_ITEM_INDEX = 1;
    private static final int SECOND_ITEM_INDEX = 2;
    private static final int REF_TO_NEXT_INDEX = 0;
    private static final int FIRST_OFFSET_INDEX = BUNCH_SIZE;


    private int nextSubqueueIndex;
    private volatile ProducerSubqueue[] producers;

    public MPSCLinkedMultiplexer() {

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
        for (;;) {
            ProducerSubqueue<T> producer = producers[nextSubqueueIndex % localProducers.length];

            if (producer.headIndex == FIRST_OFFSET_INDEX) {
                Object[] newHeadBunch = (Object[]) producer.headBunch[REF_TO_NEXT_INDEX];
                if (newHeadBunch == null) {
                    continue;
                }

                producer.headIndex = FIRST_ITEM_INDEX;
                producer.headBunch = newHeadBunch;
            }

            Object element = producer.headBunch[producer.headIndex];
            if (element != null) {
                producer.headIndex++;
                return (T) element;
            }

            nextSubqueueIndex++;
        }

        return null;
    }


    private static class ProducerSubqueue<T> implements QueueTail<T> {

        private int headIndex;
        private Object[] headBunch;

        private int tailIndex;
        private Object[] tailBunch;

        private ProducerSubqueue() {
            Object[] firstBunch = new Object[BUNCH_SIZE];
            headBunch = firstBunch;
            tailBunch = firstBunch;
            headIndex = FIRST_ITEM_INDEX;
            tailIndex = FIRST_ITEM_INDEX;
        }

        @Override
        public void addToTail(T element) {
            if (tailIndex == FIRST_OFFSET_INDEX) {
                Object[] newTailBunch = new Object[BUNCH_SIZE];
                newTailBunch[REF_TO_NEXT_INDEX] = null;
                newTailBunch[FIRST_ITEM_INDEX] = element;
                tailBunch[REF_TO_NEXT_INDEX] = newTailBunch;
                tailBunch = newTailBunch;
                tailIndex = SECOND_ITEM_INDEX;

                return;
            }

            tailBunch[tailIndex++] = element;
        }
    }
}
