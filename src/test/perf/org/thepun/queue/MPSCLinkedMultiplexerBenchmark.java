package org.thepun.queue;

import java.util.concurrent.CountDownLatch;

import org.thepun.queue.mpsc.MPSCLinkedMultiplexer;

public class MPSCLinkedMultiplexerBenchmark {

    private static final int PRODUCERS = 3;
    private static final int N = 100_000_000 / PRODUCERS;

    private static final Long[] values;
    static {
        values = new Long[N];
        for (int l = 0; l < N; l++) {
            values[l] = new Long(l);
        }
    }

    private static volatile long result;

    public static void main(String[] args) throws InterruptedException {
        final MPSCLinkedMultiplexer<Long> queue = new MPSCLinkedMultiplexer<>();

        final QueueTail<Long>[] entry = new QueueTail[PRODUCERS];
        for (int m = 0; m < PRODUCERS; m++) {
            entry[m] = queue.createProducer();
        }

        for (int k = 0; k < 1000; k++) {
            final CountDownLatch START = new CountDownLatch(1);
            final CountDownLatch FINISH = new CountDownLatch(1 + PRODUCERS);

            class ProducerThraed extends Thread {
                private final QueueTail<Long> producer;

                ProducerThraed(QueueTail<Long> producer) {
                    this.producer = producer;
                }

                @Override
                public void run() {
                    try {
                        START.await();
                    } catch (InterruptedException e) {
                        return;
                    }

                    for (int i = 0; i < N; i++) {
                        producer.addToTail(values[i]);
                    }

                    FINISH.countDown();
                }
            }

            class ConsumerThread extends Thread {
                @Override
                public void run() {
                    try {
                        START.await();
                    } catch (InterruptedException e) {
                        return;
                    }

                    Long value;

                    long tempValue = 0;
                    for (int i = 0; i < N * PRODUCERS; i++) {
                        do {
                            value = queue.removeFromHead();
                        } while (value == null);

                        tempValue += value.longValue();
                    }

                    result = tempValue;
                    FINISH.countDown();
                }
            }

            ProducerThraed[] producers = new ProducerThraed[PRODUCERS];
            for (int i = 0; i < PRODUCERS; i++) {
                producers[i] = new ProducerThraed(entry[i]);
                producers[i].start();
            }

            ConsumerThread consumerThread = new ConsumerThread();
            consumerThread.start();

            System.out.println("Started!");

            long startTime = System.nanoTime();
            START.countDown();
            FINISH.await();
            long finishTime = System.nanoTime();

            System.out.println("Time: " + ((finishTime - startTime) / 1_000_000) + "ms");
            System.out.println("Final result: " + result);

            //System.gc();
            Thread.sleep(1000);
        }
    }


}
