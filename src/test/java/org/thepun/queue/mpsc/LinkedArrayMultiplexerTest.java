package org.thepun.queue.mpsc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.thepun.queue.QueueHead;
import org.thepun.queue.QueueTail;
import org.thepun.queue.TestUtils;

public class LinkedArrayMultiplexerTest {

    private static final int PRODUCERS = 3;
    private static final int N = 10_000_000;
    private static final long TOTAL = TestUtils.calcTotal(N, PRODUCERS);

    private static volatile long result;

    @Test
    public void addAndGet() throws InterruptedException {
        LinkedArrayMultiplexer<Long> queue = new LinkedArrayMultiplexer<>();

        for (int k = 0; k < 100; k++) {
            result = 0;

            ProducerThraed[] producers = new ProducerThraed[PRODUCERS];
            for (int i = 0; i < PRODUCERS; i++) {
                producers[i] = new ProducerThraed(queue.createProducer(), i);
                producers[i].start();
            }

            ConsumerThread consumerThread = new ConsumerThread(queue);
            consumerThread.setPriority(Thread.MAX_PRIORITY);
            consumerThread.start();

            for (int i = 0; i < PRODUCERS; i++) {
                producers[i].join();
            }
            consumerThread.join();

            assertEquals(TOTAL, result);

            System.out.println("Iteration done! " + (k + 1) + " of 100");
        }
    }


    private static class ProducerThraed extends Thread {

        private final int offset;
        private final QueueTail<Long> producer;

        ProducerThraed(QueueTail<Long> producer, int offset) {
            this.producer = producer;
            this.offset = offset;
        }

        @Override
        public void run() {
            for (long i = 0; i < N; i++) {
                producer.addToTail(i * PRODUCERS + offset);
            }
        }
    }


    private static class ConsumerThread extends Thread {

        private final Long[] counters;
        private final QueueHead<Long> consumer;

        ConsumerThread(QueueHead<Long> consumer) {
            this.consumer = consumer;

            counters = new Long[PRODUCERS];
            for (int i = 0; i < PRODUCERS; i++) {
                counters[i] = (long) i;
            }
        }

        @Override
        public void run() {
            long tempValue = 0;
            for (long i = 0; i < N * PRODUCERS; i++) {
                Long value;
                do {
                    value = consumer.removeFromHead();
                } while (value == null);

                int offset = (int)(value % PRODUCERS);
                long nextValue = counters[offset];
                assertEquals(nextValue, (long) value);
                counters[offset] = nextValue + PRODUCERS;

                tempValue += value;
            }
            result += tempValue;
        }
    }
}
