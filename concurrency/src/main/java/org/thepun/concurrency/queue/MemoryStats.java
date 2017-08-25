package org.thepun.concurrency.queue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.openjdk.jol.vm.VM;

public final class MemoryStats {

    private final ArrayList<MemoryStatsRecord> records;

    public MemoryStats() {
        records = new ArrayList<>(1000);
    }

    public void addRecord(Object object) {
        long address = VM.current().addressOf(object);
        long size = VM.current().sizeOf(object);
        records.add(new MemoryStatsRecord(address, size));
    }

    public void print() {
        Map<Long, Integer> pages = new HashMap<>();
        Map<Long, Integer> cacheLines = new HashMap<>();

        records.forEach(record -> {
            long alignedCacheAddress = record.getAddress() / 64 * 64;
            do {
                cacheLines.compute(alignedCacheAddress, (key, value) -> {
                    if (value == null) {
                        return 1;
                    } else {
                        return value + 1;
                    }
                });

                alignedCacheAddress += 64;
            } while (alignedCacheAddress < record.getAddress() + record.getSize());

            long alignedPageAddress = record.getAddress() / 4096 * 4096;
            do {
                pages.compute(alignedPageAddress, (key, value) -> {
                    if (value == null) {
                        return 1;
                    } else {
                        return value + 1;
                    }
                });

                alignedPageAddress += 4096;
            } while (alignedPageAddress < record.getAddress() + record.getSize());
        });

        System.out.println();

        System.out.println("Objects: ");
        System.out.println("count " + records.size());
        System.out.println("total size " + records.stream().map(MemoryStatsRecord::getSize).mapToLong(Long::longValue).sum());

        System.out.println();

        System.out.println("Cache lines: ");
        System.out.println("count " + cacheLines.size());
        System.out.println("min duplicates " + cacheLines.values().stream().min(Comparator.naturalOrder()).orElse(0));
        System.out.println("max duplicates " + cacheLines.values().stream().max(Comparator.naturalOrder()).orElse(0));

        System.out.println();

        System.out.println("Pages: ");
        System.out.println("count " + pages.size());
        System.out.println("min duplicates " + pages.values().stream().min(Comparator.naturalOrder()).orElse(0));
        System.out.println("max duplicates " + pages.values().stream().max(Comparator.naturalOrder()).orElse(0));

        System.out.println();
    }
}
