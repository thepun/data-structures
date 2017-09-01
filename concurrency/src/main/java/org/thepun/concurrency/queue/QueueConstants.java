package org.thepun.concurrency.queue;

import org.thepun.unsafe.ArrayMemoryLayout;

final class QueueConstants {

    static final int LINKED_BUNCH_SIZE = 1024;
    static final Object[] LINKED_NULLS_BUNCH = new Object[LINKED_BUNCH_SIZE];
    static final int LINKED_FIRST_OFFSET_INDEX = LINKED_BUNCH_SIZE;
    static final int LINKED_FIRST_ITEM_INDEX = 1;
    static final int LINKED_SECOND_ITEM_INDEX = 2;
    static final long LINKED_FIRST_ITEM_INDEX_ADDRESS = ArrayMemoryLayout.getElementOffset(Object[].class, 1);
    static final long LINKED_REF_TO_NEXT_INDEX_ADDRESS = ArrayMemoryLayout.getElementOffset(Object[].class, 0);

}
