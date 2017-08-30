package org.thepun.concurrency.queue;

import java.util.concurrent.CountDownLatch;


class BenchmarkCases {

    static long singleProducerAndSingleConsumer(QueueHead<Long> queueHead, QueueTail<Long> queueTail, Long[] values, int count) throws InterruptedException {
        QueueHead<Long>[] queueHeads = new QueueHead[1];
        queueHeads[0] = queueHead;
        return singleProducerAndMultipleConsumers(queueHeads, queueTail, values, count);
    }

    static long singleProducerAndMultipleConsumers(QueueHead<Long>[] queueHeads, QueueTail<Long> queueTail, Long[] values, int count) throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1 + queueHeads.length);

        ProducerThraed producerThraed = new ProducerThraed(startLatch, finishLatch, queueTail, values, count / queueHeads.length * queueHeads.length);

        ConsumerThread[] consumerThreads = new ConsumerThread[queueHeads.length];
        for (int i = 0; i < consumerThreads.length; i++) {
            consumerThreads[i] = new ConsumerThread(startLatch, finishLatch, queueHeads[i], count / queueHeads.length);
        }

        producerThraed.start();
        for (int i = 0; i < consumerThreads.length; i++) {
            consumerThreads[i].start();
        }

        startLatch.countDown();
        finishLatch.await();

        long result = 0;
        for (int i = 0; i < consumerThreads.length; i++) {
            result =+ consumerThreads[i].getResult();
        }
        return result;
    }

    static long multipleProducersAndSingleConsumers(QueueHead<Long> queueHead, QueueTail<Long>[] queueTails, Long[] values, int count) throws
            InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1 + queueTails.length);

        ProducerThraed[] producerThraeds = new ProducerThraed[queueTails.length];
        ConsumerThread consumerThread = new ConsumerThread(startLatch, finishLatch, queueHead, count / queueTails.length * queueTails.length);

        for (int i = 0; i < producerThraeds.length; i++) {
            producerThraeds[i] = new ProducerThraed(startLatch, finishLatch, queueTails[i], values, count / queueTails.length);
        }

        for (int i = 0; i < producerThraeds.length; i++) {
            producerThraeds[i].start();
        }
        consumerThread.start();

        startLatch.countDown();
        finishLatch.await();

        return consumerThread.getResult();
    }
}
