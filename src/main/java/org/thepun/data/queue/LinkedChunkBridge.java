package org.thepun.data.queue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.thepun.unsafe.ArrayMemory;

/**
 * Single producer / single consumer queue implementation based on linked list.
 *
 * GUARANTIES:
 * 1. Ultra fast
 * 2. Wait-free
 * 3. Almost no atomic operations (only during buffer allocation once in ~1K elements)
 * 4. FIFO order
 * 5. push / pop crosses a memory barrier
 * 6. Do not produce garbage
 *
 * LIMITATIONS:
 * 1. Unbounded
 * 2. No guaranties on behavior outside of initial producer/consumer threads
 * 3. Latency can significantly plunge during buffer allocation (after each ~1K elements)
 * 4. Works only with architecture with strong memory consistency (i.e. x86)
 *
 * @param <T> type of objects to store
 */
public final class LinkedChunkBridge<T> implements QueueHead<T>, QueueTail<T> {

    private final AlignedLinkedNode head;
    private final AlignedLinkedNode tail;
    private final AtomicReference<Object[]> emptyChain;

    public LinkedChunkBridge() {
        Object[] firstBunch = new Object[QueueConstants.LINKED_BUNCH_SIZE];
        head = new AlignedLinkedNode();
        tail = new AlignedLinkedNode();
        head.bunch = firstBunch;
        tail.bunch = firstBunch;
        head.index = QueueConstants.LINKED_FIRST_ITEM_INDEX;
        tail.index = QueueConstants.LINKED_FIRST_ITEM_INDEX;
        emptyChain = new AtomicReference<>();
    }

    @Override
    public boolean addToTail(T element) {
        int localIndex = tail.index;
        Object[] localBunch = tail.bunch;
        if (localIndex == QueueConstants.LINKED_FIRST_OFFSET_INDEX) {
            Object[] localEmptyChain = tail.emptyChain;
            if (localEmptyChain == null) {
                Object[] newChain = emptyChain.getAndSet(null);
                if (newChain == null) {
                    newChain = new Object[QueueConstants.LINKED_BUNCH_SIZE];
                }

                localEmptyChain = newChain;
            }

            ArrayMemory.setObject(localEmptyChain, QueueConstants.LINKED_FIRST_ITEM_INDEX_ADDRESS, element);

            tail.emptyChain = (Object[]) ArrayMemory.getObject(localEmptyChain, QueueConstants.LINKED_REF_TO_NEXT_INDEX_ADDRESS);
            ArrayMemory.setObject(localEmptyChain, QueueConstants.LINKED_REF_TO_NEXT_INDEX_ADDRESS, null);
            ArrayMemory.setObject(localBunch, QueueConstants.LINKED_REF_TO_NEXT_INDEX_ADDRESS, localEmptyChain);
            tail.bunch = localEmptyChain;
            tail.index = QueueConstants.LINKED_SECOND_ITEM_INDEX;
            return true;
        }

        ArrayMemory.setObject(localBunch, localIndex, element);
        tail.index = localIndex + 1;
        return true;
    }

    @Override
    public T removeFromHead() {
        int localIndex = head.index;
        Object[] localBunch = head.bunch;
        if (localIndex == QueueConstants.LINKED_FIRST_OFFSET_INDEX) {
            Object[] oldHeadBunh = localBunch;
            localBunch = (Object[]) ArrayMemory.getObject(localBunch, QueueConstants.LINKED_REF_TO_NEXT_INDEX_ADDRESS);
            if (localBunch == null) {
                // no more bunches at the moment
                return null;
            }

            // change current bunch to the next one
            head.bunch = localBunch;
            head.index = QueueConstants.LINKED_FIRST_ITEM_INDEX;
            localIndex = QueueConstants.LINKED_FIRST_ITEM_INDEX;

            // clear array from reader thread to be sure about initial state without fences
            System.arraycopy(QueueConstants.LINKED_NULLS_BUNCH, 0, oldHeadBunh, 0, QueueConstants.LINKED_BUNCH_SIZE);

            // check if writer took all freed bunches
            Object[] prevEmptyChainHead = emptyChain.get();
            if (prevEmptyChainHead == null) {
                // we need to cross fence to be able to rely on it
                emptyChain.set(oldHeadBunh);
            } else {
                // add empty bunch to list
                ArrayMemory.setObject(oldHeadBunh, QueueConstants.LINKED_REF_TO_NEXT_INDEX_ADDRESS, prevEmptyChainHead);

                // if writer took empty bunches
                if (!emptyChain.compareAndSet(prevEmptyChainHead, oldHeadBunh)) {
                    // ensure initial state is written by reader thread
                    ArrayMemory.setObject(oldHeadBunh, QueueConstants.LINKED_REF_TO_NEXT_INDEX_ADDRESS, null);
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
        long start = System.nanoTime();
        long finish = start + timeUnit.toNanos(timeout);

        T element;
        for (;;) {
            element = removeFromHead();
            if (element != null) {
                return element;
            }

            if (System.nanoTime() > finish) {
                throw new TimeoutException();
            }

            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }
}
