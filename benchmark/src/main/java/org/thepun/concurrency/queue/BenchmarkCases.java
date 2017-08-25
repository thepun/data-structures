package org.thepun.concurrency.queue;

import java.util.concurrent.CountDownLatch;

import org.thepun.queue.QueueHead;
import org.thepun.queue.QueueTail;

class BenchmarkCases {

    static long singlewProducerAndSingleConsumer(QueueHead<Long> queueHead, QueueTail<Long> queueTail, Long[] values) throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);

        ProducerThraed producerThraed = new ProducerThraed(startLatch, finishLatch, queueTail, values);
        ConsumerThread consumerThread = new ConsumerThread(startLatch, finishLatch, queueHead, values.length);
        producerThraed.start();
        consumerThread.start();

        startLatch.countDown();
        finishLatch.await();

        return consumerThread.getResult();
    }
}
