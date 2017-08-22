package org.thepun.queue.spsc;

import java.util.concurrent.atomic.AtomicReference;

import org.thepun.queue.SimpleQueue;

import sun.misc.Contended;

@SuppressWarnings("unchecked")
public class SPSCSplittedLinkedQueue<T> implements SimpleQueue<T> {

    private static final int BUNCH_SIZE = 256;
    private static final int FIRST_ITEM_INDEX = 1;
    private static final int SECOND_ITEM_INDEX = 2;
    private static final int REF_TO_NEXT_INDEX = 0;
    private static final int FIRST_OFFSET_INDEX = BUNCH_SIZE;

    private static final Object[] EMPTY_ARRAY = new Object[BUNCH_SIZE];


    private final CurrentBlock head;
    private final CurrentBlock tail;
    private final AtomicReference<Object[]> emptyChain;

    public SPSCSplittedLinkedQueue() {
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
        if (tail.index == FIRST_OFFSET_INDEX) {
            Object[] newTailBunch;
            
            if (tail.emptyChain == null) {
                Object[] newChain = emptyChain.getAndSet(null);
                if (newChain == null) {
                    newChain = new Object[BUNCH_SIZE];
                }
                
                tail.emptyChain = newChain;
            }

            newTailBunch = tail.emptyChain;
            tail.emptyChain = (Object[]) tail.emptyChain[REF_TO_NEXT_INDEX];
            newTailBunch[REF_TO_NEXT_INDEX] = null;
            newTailBunch[FIRST_ITEM_INDEX] = element;
            tail.bunch[REF_TO_NEXT_INDEX] = newTailBunch;
            tail.bunch = newTailBunch;
            tail.index = SECOND_ITEM_INDEX;

            return;
        }

        tail.bunch[tail.index++] = element;
    }

    @Override
    public T removeFromHead() {
        if (head.index == FIRST_OFFSET_INDEX) {
            Object[] newHeadBunch = (Object[]) head.bunch[REF_TO_NEXT_INDEX];
            if (newHeadBunch == null) {
                return null;
            }

            Object[] oldHeadBunh = head.bunch;
            head.index = FIRST_ITEM_INDEX;
            head.bunch = newHeadBunch;

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

        Object element = head.bunch[head.index];
        if (element != null) {
            head.index++;
        }

        return (T) element;
    }


    @Contended
    private static final class CurrentBlock {
        private int index;
        private Object[] bunch;
        private Object[] emptyChain;
    }
}
