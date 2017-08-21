package org.thepun.queue.spsc;

import org.thepun.queue.SimpleQueue;

import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unchecked")
public class SPSCLinkedQueue<T> implements SimpleQueue<T> {

    private static final int BUNCH_SIZE = 256;
    private static final int FIRST_ITEM_INDEX = 1;
    private static final int SECOND_ITEM_INDEX = 2;
    private static final int REF_TO_NEXT_INDEX = 0;
    private static final int FIRST_OFFSET_INDEX = BUNCH_SIZE;
    private static final Object[] EMPTY_ARRAY = new Object[BUNCH_SIZE];


    private int headIndex;
    private Object[] headBunch;

    private int tailIndex;
    private Object[] tailBunch;
    private Object[] tailEmptyChain;

    private final AtomicReference<Object[]> emptyChain;

    public SPSCLinkedQueue() {
        Object[] firstBunch = new Object[BUNCH_SIZE];
        headBunch = firstBunch;
        tailBunch = firstBunch;
        headIndex = FIRST_ITEM_INDEX;
        tailIndex = FIRST_ITEM_INDEX;
        emptyChain = new AtomicReference<>();
    }

    @Override
    public void addToTail(T element) {
        if (tailIndex == FIRST_OFFSET_INDEX) {
            Object[] newTailBunch;
            
            if (tailEmptyChain == null) {
                Object[] newChain = emptyChain.getAndSet(null);
                if (newChain == null) {
                    newChain = new Object[BUNCH_SIZE];
                }
                
                tailEmptyChain = newChain;
            }

            newTailBunch = tailEmptyChain;
            tailEmptyChain = (Object[]) tailEmptyChain[REF_TO_NEXT_INDEX];
            newTailBunch[REF_TO_NEXT_INDEX] = null;
            newTailBunch[FIRST_ITEM_INDEX] = element;
            tailBunch[REF_TO_NEXT_INDEX] = newTailBunch;
            tailBunch = newTailBunch;
            tailIndex = SECOND_ITEM_INDEX;

            return;
        }

        tailBunch[tailIndex++] = element;
    }

    @Override
    public T removeFromHead() {
        if (headIndex == FIRST_OFFSET_INDEX) {
            Object[] newHeadBunch = (Object[]) headBunch[REF_TO_NEXT_INDEX];
            if (newHeadBunch == null) {
                return null;
            }

            Object[] oldHeadBunh = headBunch;
            headIndex = FIRST_ITEM_INDEX;
            headBunch = newHeadBunch;

            System.arraycopy(EMPTY_ARRAY, 0, oldHeadBunh, 0, BUNCH_SIZE);

            Object[] prevEmptyChainHead = emptyChain.get();
            if (prevEmptyChainHead == null) {
                emptyChain.lazySet(oldHeadBunh);
            } else {
                oldHeadBunh[REF_TO_NEXT_INDEX] = prevEmptyChainHead;
                if (!emptyChain.compareAndSet(prevEmptyChainHead, oldHeadBunh)) {
                    oldHeadBunh[REF_TO_NEXT_INDEX] = null;
                    emptyChain.lazySet(oldHeadBunh);
                }
            }
        }

        Object element = headBunch[headIndex];
        if (element != null) {
            headIndex++;
        }

        return (T) element;
    }
}
