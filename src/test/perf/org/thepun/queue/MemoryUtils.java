package org.thepun.queue;

import java.util.HashMap;
import java.util.Map;

import org.openjdk.jol.info.GraphLayout;

class MemoryUtils {

    public static void printMemoryLayoutInformation(Object object) {
        GraphLayout graphLayout = GraphLayout.parseInstance(object);

        Map<Long, Integer> pages = new HashMap<>();
        Map<Long, Integer> cacheLines = new HashMap<>();

        graphLayout.addresses().stream()
            .map(graphLayout::record)
            .forEach(record -> {
                long alignedCacheAddress = record.address() / 64 * 64;
                cacheLines.compute(alignedCacheAddress, (key, value) -> {
                    if (value == null) {
                        return 1;
                    } else {
                        return value + 1;
                    }
                });

                long alignedPageAddress = record.address() / 4096 * 4096;
                pages.compute(alignedPageAddress, (key, value) -> {
                    if (value == null) {
                        return 1;
                    } else {
                        return value + 1;
                    }
                });
            });

        System.out.println("Cache lines: ");
        cacheLines.forEach((key, value) -> System.out.println(Long.toHexString(key) + ": " + value));
        System.out.println();

        System.out.println("Pages: ");
        pages.forEach((key, value) -> System.out.println(Long.toHexString(key) + ": " + value));
        System.out.println();
    }

}
