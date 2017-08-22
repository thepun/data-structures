package org.thepun.queue;

import java.util.concurrent.CountDownLatch;

class BenchmarkCases {

    static void singlewProducerAndSingleConsumer(QueueHead<Long> queueHead, QueueTail<Long> queueTail, Long[] values) throws InterruptedException {
        System.out.println("New iteration!");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);

        ProducerThraed producerThraed = new ProducerThraed(startLatch, finishLatch, queueTail, values);
        ConsumerThread consumerThread = new ConsumerThread(startLatch, finishLatch, queueHead, values.length);
        producerThraed.start();
        consumerThread.start();

        System.out.println("Started!");

        long startTime = System.nanoTime();
        startLatch.countDown();
        finishLatch.await();
        long finishTime = System.nanoTime();

        System.out.println("Time: " + ((finishTime - startTime) / 1_000_000) + "ms");
        System.out.println("Final result: " + consumerThread.getResult());

        //System.gc();
        Thread.sleep(1000);
    }
}
