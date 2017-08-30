package org.thepun.concurrency.queue;

import org.thepun.unsafe.Atomic;
import org.thepun.unsafe.ObjectMemoryLayout;
import org.thepun.unsafe.Volatile;

final class AlignedLong {

    private static final long valueOffset;
    static {
        valueOffset = ObjectMemoryLayout.getFieldMemoryOffset(AlignedLong.class, "value");
    }


    // 12 bytes header
    // 52 bytes gap before
    private int before1, before2,
            before3, before4, before5,
            before6, before7, before8,
            before9, before10, before11,
            before12, before13;

    private long value;

    // 56 bytes gap
    private long after1, after2,
            after3, after4, after5,
            after6, after7;

    // 64 bytes gap
    private long after8, after9,
            after10, after11, after12,
            after13, after14, after15;


    public long get() {
        return value;
    }

    public void set(long newValue) {
        value = newValue;
    }

    public long volatileGet() {
        return Volatile.getLong(this, valueOffset);
    }

    public void volatileSet(long newValue) {
        Volatile.setLong(this, valueOffset, newValue);
    }

    public void increment() {
        long current;
        do {
            current = value;
        } while (!Atomic.compareAndSwapLong(this, valueOffset, current, current + 1L));
    }

    public long getAndIncrement(long upperLimit) {
        long current;
        do {
            current = value;
            if (current >= upperLimit) {
                return -1;
            }
        } while (!compareAndSwap(current, current + 1L));

        return current;
    }

    public boolean compareAndSwap(long expectedValue, long newValue) {
        return Atomic.compareAndSwapLong(this, valueOffset, expectedValue, newValue);
    }
}
