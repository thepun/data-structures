package org.thepun.queue;

import org.thepun.queue.spsc.LinkedArrayQueue;

public class SPSCLinkedQueueBenchmark {

    static final int N = 100_000_000;


    public static void main(String[] args) throws InterruptedException {
        final Long[] values = new Long[N];
        for (int l = 0; l < N; l++) {
            values[l] = new Long(l);
        }

        System.out.println("Initialized!");

        LinkedArrayQueue<Long> queue;

        queue = new LinkedArrayQueue<>();
        BenchmarkCases.singlewProducerAndSingleConsumer(queue, queue, values);

        queue = new LinkedArrayQueue<>();
        BenchmarkCases.singlewProducerAndSingleConsumer(queue, queue, values);

        queue = new LinkedArrayQueue<>();
        BenchmarkCases.singlewProducerAndSingleConsumer(queue, queue, values);

        queue = new LinkedArrayQueue<>();
        BenchmarkCases.singlewProducerAndSingleConsumer(queue, queue, values);

        queue = new LinkedArrayQueue<>();
        BenchmarkCases.singlewProducerAndSingleConsumer(queue, queue, values);

        queue = new LinkedArrayQueue<>();
        BenchmarkCases.singlewProducerAndSingleConsumer(queue, queue, values);

        queue = new LinkedArrayQueue<>();
        BenchmarkCases.singlewProducerAndSingleConsumer(queue, queue, values);

        queue = new LinkedArrayQueue<>();
        BenchmarkCases.singlewProducerAndSingleConsumer(queue, queue, values);

        queue = new LinkedArrayQueue<>();
        BenchmarkCases.singlewProducerAndSingleConsumer(queue, queue, values);

        queue = new LinkedArrayQueue<>();
        BenchmarkCases.singlewProducerAndSingleConsumer(queue, queue, values);

        queue = new LinkedArrayQueue<>();
        BenchmarkCases.singlewProducerAndSingleConsumer(queue, queue, values);

    }

}
