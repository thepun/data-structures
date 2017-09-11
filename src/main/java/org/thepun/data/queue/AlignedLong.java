package org.thepun.data.queue;

import org.thepun.unsafe.ObjectMemory;
import org.thepun.unsafe.ObjectMemoryLayout;

/**
 * Internal class for storing long value aligned to cache lines for lesser false sharing.
 *
 * Variable that stores the value is not volatile. Use volatileGet / volatileSet methods to cross memory barriers.
 *
 * Provides Compare-and-Swap (CAS) operation.
 */
final class AlignedLong {

    private static final long valueOffset;
    static {
        valueOffset = ObjectMemoryLayout.getFieldOffset(AlignedLong.class, "value");
    }


    // 12 bytes header
    // 52 bytes gap before
    private int before1, before2,
            before3, before4, before5,
            before6, before7, before8,
            before9, before10, before11,
            before12, before13;

    // non-volatile field to store current value
    private long value;

    // 56 bytes gap
    private long after1, after2,
            after3, after4, after5,
            after6, after7;

    // 64 bytes gap
    private long after8, after9,
            after10, after11, after12,
            after13, after14, after15;


    long get() {
        return value;
    }

    void set(long newValue) {
        value = newValue;
    }

    boolean compareAndSwap(long expectedValue, long newValue) {
        return ObjectMemory.compareAndSwapLong(this, valueOffset, expectedValue, newValue);
    }

    long getAndIncrement() {
        return ObjectMemory.getAndIncrementLong(this, valueOffset, 1L);
    }
}
