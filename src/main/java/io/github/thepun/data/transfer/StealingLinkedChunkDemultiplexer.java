package io.github.thepun.data.transfer;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.thepun.unsafe.ArrayMemory;
import org.thepun.unsafe.ArrayMemoryLayout;

public final class StealingLinkedChunkDemultiplexer<T> implements QueueTail<T>, HasConsumers<T> {

    // TODO: align consumer variables

    private static final int LINKED_BUNCH_SIZE = 1024;
    private static final int LINKED_FIRST_OFFSET_INDEX = LINKED_BUNCH_SIZE - 2;
    private static final int LINKED_FIRST_ITEM_INDEX = 0;
    private static final int LINKED_SECOND_ITEM_INDEX = 1;
    private static final long LINKED_FIRST_ITEM_INDEX_ADDRESS = ArrayMemoryLayout.getElementOffset(Object[].class, 0);
    private static final long LINKED_REF_TO_NEXT_INDEX_ADDRESS = ArrayMemoryLayout.getElementOffset(Object[].class, LINKED_BUNCH_SIZE - 1);
    private static final long LINKED_REF_TO_NEXT_GC_INDEX_ADDRESS = ArrayMemoryLayout.getElementOffset(Object[].class, LINKED_BUNCH_SIZE - 2);
    private static final Object[] LINKED_NULLS_BUNCH = new Object[LINKED_BUNCH_SIZE];
    private static final Object EMPTY_REF = new Object();
    private static final Object STEAL_REF = new Object();


    private final AtomicReference<Object[]> globalEmptyChain;

    private int nextConsumerIndex;
    private Object[] writerEmptyChain;
    private StealingConsumer<T>[] consumers;

    public StealingLinkedChunkDemultiplexer() {
        consumers = new StealingConsumer[0];
        globalEmptyChain = new AtomicReference<>(null);
    }

    @Override
    public synchronized QueueHead<T> createConsumer() {
        StealingConsumer<T>[] oldConsumers = consumers;
        StealingConsumer<T>[] newConsumers = Arrays.copyOf(oldConsumers, oldConsumers.length + 1);
        StealingConsumer<T> consumer = new StealingConsumer<>(this);
        newConsumers[oldConsumers.length] = consumer;
        updateConsumers(newConsumers);
        return consumer;
    }

    @Override
    public synchronized void destroyConsumer(QueueHead<T> consumer) {
        if (!(consumer instanceof StealingLinkedChunkDemultiplexer.StealingConsumer)) {
            throw new IllegalArgumentException("Wrong consumer");
        }

        StealingConsumer<T> producerSubqueue = (StealingConsumer<T>) consumer;
        if (producerSubqueue.parent != this) {
            throw new IllegalArgumentException("Consumer from another router");
        }

        StealingConsumer<T>[] newConsumers;
        StealingConsumer<T>[] oldConsumers;

        oldConsumers = consumers;
        int index = -1;
        for (int i = 0; i < oldConsumers.length; i++) {
            if (oldConsumers[i] == consumer) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            throw new IllegalArgumentException("Consumer not found");
        }

        newConsumers = new StealingConsumer[oldConsumers.length - 1];
        System.arraycopy(oldConsumers, 0, newConsumers, 0, index);
        System.arraycopy(oldConsumers, index + 1, newConsumers, index + 1 - 1, oldConsumers.length - (index + 1));
        updateConsumers(newConsumers);
    }

    private void updateConsumers(StealingConsumer<T>[] newConsumers) {
        consumers = newConsumers;

        for (int i = 0; i < consumers.length; i++) {
            final int finalI = i;

            StealingConsumer<T> thisConsumer = consumers[finalI];

            StealingConsumer<T>[] otherConsumers = Stream.of(this.consumers)
                    .filter(consumer -> consumer != thisConsumer)
                    .toArray(length -> new StealingConsumer[length]);

            thisConsumer.consumers = otherConsumers;
        }
    }

    @Override
    public boolean addToTail(T element) {
        int localNextConsumerIndex = nextConsumerIndex;
        StealingConsumer<T>[] localConsumers = consumers;
        int numberOfConsumers = localConsumers.length;

        StealingConsumer<T> consumer = localConsumers[localNextConsumerIndex % numberOfConsumers];
        AlignedBunch currentWriteNode = consumer.currentWriteNode;
        Object[] currentBunch = currentWriteNode.bunch;
        int currentIndex = currentWriteNode.index;
        if (currentIndex == LINKED_FIRST_OFFSET_INDEX) {
            //Object[] localEmptyChain = new Object[LINKED_BUNCH_SIZE];
            Object[] localEmptyChain = writerEmptyChain;
            if (localEmptyChain == null) {
                Object[] newChain = globalEmptyChain.getAndSet(null);
                if (newChain == null) {
                    newChain = new Object[LINKED_BUNCH_SIZE];
                }

                localEmptyChain = newChain;
                //localEmptyChain = new Object[LINKED_BUNCH_SIZE];
            }

            writerEmptyChain = (Object[]) ArrayMemory.getObject(localEmptyChain, LINKED_REF_TO_NEXT_GC_INDEX_ADDRESS);
            ArrayMemory.setObject(localEmptyChain, LINKED_FIRST_ITEM_INDEX_ADDRESS, element);
            ArrayMemory.setObject(currentBunch, LINKED_REF_TO_NEXT_INDEX_ADDRESS, localEmptyChain);
            currentWriteNode.index = LINKED_SECOND_ITEM_INDEX;
            currentWriteNode.bunch = localEmptyChain;
            nextConsumerIndex = localNextConsumerIndex + 1;
            return true;
        }

        ArrayMemory.setObject(currentBunch, currentIndex, element);
        currentWriteNode.index = currentIndex + 1;
        nextConsumerIndex = localNextConsumerIndex + 1;
        return true;
    }


    private static final class StealingConsumer<T> implements QueueHead<T> {

        private final StealingLinkedChunkDemultiplexer<T> parent;

        private final AlignedLong nextConsumerToStealFrom;
        private final AlignedBunch currentReadNode;
        private final AlignedBunch currentWriteNode;
        private final AtomicReference<Object[]> emptyChain;

        private StealingConsumer<T>[] consumers;

        private StealingConsumer(StealingLinkedChunkDemultiplexer<T> parent) {
            this.parent = parent;

            emptyChain = parent.globalEmptyChain;

            Object[] firstBunch = new Object[LINKED_BUNCH_SIZE];
            currentReadNode = new AlignedBunch();
            currentWriteNode = new AlignedBunch();
            currentReadNode.index = LINKED_FIRST_ITEM_INDEX;
            currentWriteNode.index = LINKED_FIRST_ITEM_INDEX;
            currentReadNode.bunch = firstBunch;
            currentWriteNode.bunch = firstBunch;

            nextConsumerToStealFrom = new AlignedLong();
        }

        @Override
        public T removeFromHead() {
            AlignedBunch currentNode = currentReadNode;
            int currentIndex = currentNode.index;
            Object[] currentBunch = currentNode.bunch;

            // get element from owned queue
            Object element;
            for (;;) {
                if (currentIndex == LINKED_FIRST_OFFSET_INDEX) {
                    Object[] oldHeadBunh = currentBunch;
                    currentBunch = (Object[]) ArrayMemory.getObject(currentBunch, LINKED_REF_TO_NEXT_INDEX_ADDRESS);
                    if (currentBunch == null) {
                        currentNode.index = currentIndex;
                        break;
                    }

                    currentNode.bunch = currentBunch;
                    currentIndex = LINKED_FIRST_ITEM_INDEX;
                    currentNode.index = LINKED_FIRST_ITEM_INDEX;

                    // clear chunk
                    System.arraycopy(LINKED_NULLS_BUNCH, 0, oldHeadBunh, 0, LINKED_BUNCH_SIZE);

                    // return to chunk pool
                    Object[] prevEmptyChainHead;
                    do {
                        prevEmptyChainHead = emptyChain.get();
                        ArrayMemory.setObject(oldHeadBunh, LINKED_REF_TO_NEXT_GC_INDEX_ADDRESS, prevEmptyChainHead);
                    } while (!emptyChain.compareAndSet(prevEmptyChainHead, oldHeadBunh));
                }

                element = ArrayMemory.getObject(currentBunch, currentIndex);

                if (element == null) {
                    break;
                }

                if (element == EMPTY_REF) {
                    break;
                }

                // try to get element
                if (element != STEAL_REF && ArrayMemory.compareAndSwapObject(currentBunch, currentIndex, element, EMPTY_REF)) {
                    currentNode.index = currentIndex + 1;
                    return (T) element;
                }

                currentIndex++;
            }

            // go stealing
            StealingConsumer<T>[] localConsumers = consumers;
            int numberOfOtherConsumers = localConsumers.length;
            int localNextConsumerToStealFrom = (int) nextConsumerToStealFrom.get();
            for (int i = localNextConsumerToStealFrom; i < localNextConsumerToStealFrom + numberOfOtherConsumers; i++) {
                StealingConsumer<T> consumerToStealFrom = localConsumers[i % numberOfOtherConsumers];
                currentNode = consumerToStealFrom.currentReadNode;
                currentBunch = currentNode.bunch;
                currentIndex = LINKED_FIRST_OFFSET_INDEX;

                for (;;) {
                    currentIndex--;

                    element = ArrayMemory.getObject(currentBunch, currentIndex);

                    if (element == EMPTY_REF) {
                        break;
                    }

                    // try to seize element
                    if (element != null && element != STEAL_REF && ArrayMemory.compareAndSwapObject(currentBunch, currentIndex, element, STEAL_REF)) {
                        nextConsumerToStealFrom.set(localNextConsumerToStealFrom + 1);
                        return (T) element;
                    }

                    if (currentIndex == LINKED_FIRST_ITEM_INDEX) {
                        Object[] refToNextBunch = (Object[]) ArrayMemory.getObject(currentBunch, LINKED_REF_TO_NEXT_INDEX_ADDRESS);
                        if (refToNextBunch == null) {
                            break;
                        }

                        currentBunch = refToNextBunch;
                        currentIndex = LINKED_FIRST_OFFSET_INDEX;
                    }
                }
            }

            // nothing at the moment
            nextConsumerToStealFrom.set(localNextConsumerToStealFrom + 1);
            return null;
        }
    }
}
