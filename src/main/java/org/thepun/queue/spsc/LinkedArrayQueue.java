package org.thepun.queue.spsc;

import java.util.concurrent.atomic.AtomicReference;

import org.thepun.queue.SimpleQueue;
import org.thepun.unsafe.UnsafeUtils;

import sun.misc.Unsafe;

public class LinkedArrayQueue<T> implements SimpleQueue<T> {

    private static final int BUNCH_SIZE = 1024;
    private static final int FIRST_ITEM_INDEX = 1;
    private static final int SECOND_ITEM_INDEX = 2;
    private static final int FIRST_OFFSET_INDEX = BUNCH_SIZE;
    private static final int REFERENCE_SIZE = UnsafeUtils.getUnsafe().arrayIndexScale(Object[].class);
    private static final long BASE_ARRAY_OFFSET = UnsafeUtils.getUnsafe().arrayBaseOffset(Object[].class);
    private static final long FIRST_ITEM_INDEX_ADDRESS = BASE_ARRAY_OFFSET + REFERENCE_SIZE;
    private static final long REF_TO_NEXT_INDEX_ADDRESS = BASE_ARRAY_OFFSET;

    private static final Unsafe MEMORY = UnsafeUtils.getUnsafe();
    private static final Object[] NULLS = new Object[BUNCH_SIZE];


    private final CurrentBlock head;
    private final CurrentBlock tail;
    private final AtomicReference<Object[]> emptyChain;

    public LinkedArrayQueue() {
        Object[] firstBunch = new Object[BUNCH_SIZE];
        head = new CurrentBlock();
        tail = new CurrentBlock();
        head.bunch = firstBunch;
        tail.bunch = firstBunch;
        head.index = FIRST_ITEM_INDEX;
        tail.index = FIRST_ITEM_INDEX;
        emptyChain = new AtomicReference<>();
    }

    @Override
    public void addToTail(T element) {
        int localIndex = tail.index;
        Object[] localBunch = tail.bunch;
        if (localIndex == FIRST_OFFSET_INDEX) {
            Object[] localEmptyChain = tail.emptyChain;
            if (localEmptyChain == null) {
                Object[] newChain = emptyChain.getAndSet(null);
                if (newChain == null) {
                    newChain = new Object[BUNCH_SIZE];
                }

                localEmptyChain = newChain;
            }

            tail.emptyChain = (Object[]) MEMORY.getObject(localEmptyChain, REF_TO_NEXT_INDEX_ADDRESS);
            MEMORY.putObject(localEmptyChain, REF_TO_NEXT_INDEX_ADDRESS, null);
            MEMORY.putObject(localEmptyChain, FIRST_ITEM_INDEX_ADDRESS, element);
            MEMORY.putObject(localBunch, REF_TO_NEXT_INDEX_ADDRESS, localEmptyChain);
            tail.bunch = localEmptyChain;
            tail.index = SECOND_ITEM_INDEX;
            return;
        }

        MEMORY.putObject(localBunch, BASE_ARRAY_OFFSET + REFERENCE_SIZE * localIndex, element);
        tail.index = localIndex + 1;
    }

    @Override
    public T removeFromHead() {
        int localIndex = head.index;
        Object[] localBunch = head.bunch;
        if (localIndex == FIRST_OFFSET_INDEX) {
            Object[] oldHeadBunh = localBunch;
            localBunch = (Object[]) MEMORY.getObject(localBunch, REF_TO_NEXT_INDEX_ADDRESS);
            if (localBunch == null) {
                // no more bunches at the moment
                return null;
            }

            // change current bunch to the next one
            head.bunch = localBunch;
            head.index = FIRST_ITEM_INDEX;
            localIndex = FIRST_ITEM_INDEX;

            // clear array from reader thread to be sure about initial state without fences
            System.arraycopy(NULLS, 0, oldHeadBunh, 0, BUNCH_SIZE);

            // check if writer took all freed bunches
            Object[] prevEmptyChainHead = emptyChain.get();
            if (prevEmptyChainHead == null) {
                // we need to cross sfence to be able to rely on it
                emptyChain.set(oldHeadBunh);
            } else {
                // add empty bunch to list
                MEMORY.putObject(oldHeadBunh, REF_TO_NEXT_INDEX_ADDRESS, prevEmptyChainHead);

                // if writer took empty bunches
                if (!emptyChain.compareAndSet(prevEmptyChainHead, oldHeadBunh)) {
                    // ensure initial state is written by reader thread
                    MEMORY.putObject(oldHeadBunh, REF_TO_NEXT_INDEX_ADDRESS, null);
                    // again we need to cross sfence to be able to rely on it
                    emptyChain.set(oldHeadBunh);
                }
            }
        }

        Object element = MEMORY.getObject(localBunch, BASE_ARRAY_OFFSET + REFERENCE_SIZE * localIndex);
        if (element != null) {
            head.index = localIndex + 1;
        }

        return (T) element;
    }


    /**
     *  Aligned data structure for lesser false sharing
     */
    private static final class CurrentBlock {
        // 12 bytes header

        // 44 bytes gap before
        private int before1, before2,
                before3, before4, before5,
                before6, before7, before8,
                before9, before10, before11;

        private int index;
        private Object[] bunch;
        private Object[] emptyChain;

        // 60 bytes gap
        private Object after1, after2,
                after3, after4, after5,
                after6, after7, after8,
                after9, after10, after11,
                after12, after13, after14,
                after15;
    }
}
