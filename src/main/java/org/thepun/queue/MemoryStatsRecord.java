package org.thepun.queue;

public final class MemoryStatsRecord {

    private final long address;
    private final long size;

    public MemoryStatsRecord(long address, long size) {
        this.address = address;
        this.size = size;
    }

    public long getAddress() {
        return address;
    }

    public long getSize() {
        return size;
    }
}
