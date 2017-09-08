package org.thepun.data.queue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.LongAdder;


class BenchmarkCases {

    static long singleProducerAndSingleConsumer(QueueHead<Long> queueHead, QueueTail<Long> queueTail, Long[] values, int count) throws InterruptedException {
        QueueHead<Long>[] queueHeads = new QueueHead[1];
        queueHeads[0] = queueHead;
        return singleProducerAndMultipleConsumers(queueHeads, queueTail, values, count);
    }

    static long singleProducerAndMultipleConsumers(QueueHead<Long>[] queueHeads, QueueTail<Long> queueTail, Long[] values, int count) throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1 + queueHeads.length);

        int totalObjectsToPass = count / queueHeads.length * queueHeads.length;

        ProducerThraed producerThraed = new ProducerThraed(startLatch, finishLatch, queueTail, values, totalObjectsToPass);

        LongAdder adder = new LongAdder();
        ConsumerThread[] consumerThreads = new ConsumerThread[queueHeads.length];
        for (int i = 0; i < consumerThreads.length; i++) {
            consumerThreads[i] = new ConsumerThread(startLatch, finishLatch, queueHeads[i], new PerActorChecker(adder, totalObjectsToPass));
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

    static long multipleProducersAndSingleConsumer(QueueHead<Long> queueHead, QueueTail<Long>[] queueTails, Long[] values, int count) throws
            InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1 + queueTails.length);

        int countsPerProducer = count / queueTails.length;
        int totalObjectsToPass = countsPerProducer * queueTails.length;

        ProducerThraed[] producerThraeds = new ProducerThraed[queueTails.length];
        ConsumerThread consumerThread = new ConsumerThread(startLatch, finishLatch, queueHead, new TotalBasedChecker(totalObjectsToPass));

        for (int i = 0; i < producerThraeds.length; i++) {
            producerThraeds[i] = new ProducerThraed(startLatch, finishLatch, queueTails[i], values, countsPerProducer);
        }

        for (int i = 0; i < producerThraeds.length; i++) {
            producerThraeds[i].start();
        }
        consumerThread.start();

        startLatch.countDown();
        finishLatch.await();

        return consumerThread.getResult();
    }

    static long multipleProducersAndMultipleConsumer(QueueHead<Long>[] queueHeads, QueueTail<Long>[] queueTails, Long[] values, int count) throws
            InterruptedException {
        if (count / queueTails.length * queueTails.length != count / queueHeads.length * queueHeads.length) {
            throw new IllegalArgumentException("count should be divided equally by consumers and producers");
        }

        int countsPerProducer = count / queueTails.length;
        int totalObjectsToPass = countsPerProducer * queueTails.length;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1 + queueTails.length);

        ProducerThraed[] producerThraeds = new ProducerThraed[queueTails.length];
        for (int i = 0; i < producerThraeds.length; i++) {
            producerThraeds[i] = new ProducerThraed(startLatch, finishLatch, queueTails[i], values, countsPerProducer);
        }

        LongAdder adder = new LongAdder();
        ConsumerThread[] consumerThreads = new ConsumerThread[queueHeads.length];
        for (int i = 0; i < consumerThreads.length; i++) {
            consumerThreads[i] = new ConsumerThread(startLatch, finishLatch, queueHeads[i], new PerActorChecker(adder, totalObjectsToPass));
        }

        for (int i = 0; i < producerThraeds.length; i++) {
            producerThraeds[i].start();
        }
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
}
