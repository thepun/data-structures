package org.thepun.data.queue;

import org.thepun.unsafe.ArrayMemory;
import org.thepun.unsafe.ArrayMemoryLayout;
import org.thepun.unsafe.MemoryFence;

import java.util.Arrays;
import java.util.stream.Stream;

public final class GbStealingLinkedChunkDemultiplexer<T> implements Demultiplexer<T> {

    private static final Long DATA = 1L;

    private static final int LINKED_BUNCH_SIZE = 1024;
    private static final int LINKED_INDEX_STEP = 2;
    private static final int LINKED_FIRST_OFFSET_INDEX = LINKED_BUNCH_SIZE - LINKED_INDEX_STEP;
    private static final int LINKED_FIRST_ITEM_INDEX = 0;
    private static final int LINKED_SECOND_ITEM_INDEX = LINKED_INDEX_STEP;
    private static final long LINKED_FIRST_ITEM_INDEX_ADDRESS = ArrayMemoryLayout.getElementOffset(Object[].class, 0);
    private static final long LINKED_FIRST_ITEM_DATA_INDEX_ADDRESS = ArrayMemoryLayout.getElementOffset(Object[].class, 1);
    private static final long LINKED_REF_TO_NEXT_INDEX_ADDRESS = ArrayMemoryLayout.getElementOffset(Object[].class, LINKED_BUNCH_SIZE - 1);
    private static final Object[] LINKED_NULLS_BUNCH = new Object[LINKED_BUNCH_SIZE];

    private static final Object DATA_REF = new Object();
    private static final Object STEAL_REF = new Object();
    private static final Object EMPTY_REF = new Object();

    private int nextConsumerIndex;
    //private Object[] writerEmptyChain;
    private StealingConsumer<T>[] consumers;

    public GbStealingLinkedChunkDemultiplexer() {
        consumers = new StealingConsumer[0];

       // globalEmptyChain = new AtomicReference<>(null);
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
        if (!(consumer instanceof GbStealingLinkedChunkDemultiplexer.StealingConsumer)) {
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
                    .sorted((a, b) -> {
                        if (finalI % 2 == 0) {
                            return a.hashCode() - b.hashCode();
                        } else {
                            return b.hashCode() - a.hashCode();
                        }
                    })
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
        AlignedBunchReference currentWriteNode = consumer.currentWriteNode;
        Object[] currentBunch = currentWriteNode.bunch;
        int currentIndex = currentWriteNode.index;
        if (currentIndex == LINKED_FIRST_OFFSET_INDEX) {
            Object[] localEmptyChain = new Object[LINKED_BUNCH_SIZE];
            /*Object[] localEmptyChain = writerEmptyChain;
            if (localEmptyChain == null) {
                Object[] newChain = globalEmptyChain.getAndSet(null);
                if (newChain == null) {
                    newChain = new Object[LINKED_BUNCH_SIZE];
                }

                localEmptyChain = newChain;
                localEmptyChain = new Object[LINKED_BUNCH_SIZE];
            }*/

            //writerEmptyChain = (Object[]) ArrayMemory.getObject(localEmptyChain, LINKED_REF_TO_NEXT_INDEX_ADDRESS);
            ArrayMemory.setObject(localEmptyChain, LINKED_REF_TO_NEXT_INDEX_ADDRESS, null);
            //System.arraycopy(LINKED_NULLS_BUNCH, 0, localEmptyChain, 0, LINKED_BUNCH_SIZE);
            //ArrayMemory.setObject(localEmptyChain, LINKED_FIRST_ITEM_DATA_INDEX_ADDRESS, element);
            ArrayMemory.setObject(localEmptyChain, LINKED_FIRST_ITEM_INDEX_ADDRESS, DATA_REF);
            MemoryFence.store();
            ArrayMemory.setObject(currentBunch, LINKED_REF_TO_NEXT_INDEX_ADDRESS, localEmptyChain);

            currentWriteNode.index = LINKED_SECOND_ITEM_INDEX;
            currentWriteNode.bunch = localEmptyChain;
            nextConsumerIndex = localNextConsumerIndex + 1;
            return true;
        }

        //ArrayMemory.setObject(currentBunch, currentIndex + 1, element);
        //MemoryFence.store();
        ArrayMemory.setObject(currentBunch, currentIndex, DATA_REF);

        currentWriteNode.index = currentIndex + LINKED_INDEX_STEP;
        nextConsumerIndex = localNextConsumerIndex + 1;
        return true;
    }


    private static final class StealingConsumer<T> implements QueueHead<T> {

        private final GbStealingLinkedChunkDemultiplexer<T> parent;

        private final AlignedLong nextConsumerToStealFrom;
        private final AlignedBunchReference currentReadNode;
        private final AlignedBunchReference currentWriteNode;

        private StealingConsumer<T>[] consumers;

        private StealingConsumer(GbStealingLinkedChunkDemultiplexer<T> parent) {
            this.parent = parent;

            Object[] firstBunch = new Object[LINKED_BUNCH_SIZE];
            currentReadNode = new AlignedBunchReference();
            currentWriteNode = new AlignedBunchReference();
            currentReadNode.index = LINKED_FIRST_ITEM_INDEX;
            currentWriteNode.index = LINKED_FIRST_ITEM_INDEX;
            currentReadNode.bunch = firstBunch;
            currentWriteNode.bunch = firstBunch;

            nextConsumerToStealFrom = new AlignedLong();
        }

        @Override
        public T removeFromHead() {
            AlignedBunchReference currentNode = currentReadNode;
            int currentIndex = currentNode.index;
            Object[] currentBunch = currentNode.bunch;

            // get element from owned queue
            Object element;
            for (;;) {
                if (currentIndex == LINKED_FIRST_OFFSET_INDEX) {
                    Object[] refToNextBunch = (Object[]) ArrayMemory.getObject(currentBunch, LINKED_REF_TO_NEXT_INDEX_ADDRESS);
                    if (refToNextBunch == null) {
                        currentNode.index = currentIndex;
                        break;
                    }

                    currentBunch = refToNextBunch;
                    currentIndex = LINKED_FIRST_ITEM_INDEX;
                    currentNode.index = currentIndex;
                    currentNode.bunch = refToNextBunch;
                }

                element = ArrayMemory.getObject(currentBunch, currentIndex);

                // element didn't come yet
                if (element == null) {
                    break;
                }

                // element stolen
                if (element == STEAL_REF) {
                    break;
                }

                // try to get element
                element = DATA;//ArrayMemory.getObject(currentBunch, currentIndex + 1);
                if (ArrayMemory.compareAndSwapObject(currentBunch, currentIndex, DATA_REF, EMPTY_REF)) {
                    currentNode.index = currentIndex + LINKED_INDEX_STEP;
                    return (T) element;
                }

                currentIndex += LINKED_INDEX_STEP;
            }

            // go stealing
            StealingConsumer<T>[] localConsumers = consumers;
            int numberOfOtherConsumers = localConsumers.length;
            int localNextConsumerToStealFrom = (int) nextConsumerToStealFrom.get();
            for (int i = localNextConsumerToStealFrom; i < localNextConsumerToStealFrom + numberOfOtherConsumers; i++) {
                StealingConsumer<T> consumerToStealFrom = localConsumers[i % numberOfOtherConsumers];
                currentNode = consumerToStealFrom.currentReadNode;
                currentBunch = currentNode.bunch;
                currentIndex = currentNode.index;

                if (currentIndex == LINKED_FIRST_OFFSET_INDEX) {
                    continue;
                }

                /*element = ArrayMemory.getObject(currentBunch, currentIndex);
                if (element == null) {
                    continue;
                }*/

                // try to seize element
                element = DATA;//ArrayMemory.getObject(currentBunch, currentIndex + 1);
                if (ArrayMemory.compareAndSwapObject(currentBunch, currentIndex, DATA_REF, STEAL_REF)) {
                    nextConsumerToStealFrom.set(localNextConsumerToStealFrom + 1);
                    ArrayMemory.setObject(currentBunch, currentIndex, EMPTY_REF);
                    return (T) element;
                }
            }

            // nothing at the moment
            nextConsumerToStealFrom.set(localNextConsumerToStealFrom + 1);
            return null;
        }
    }
}
