package org.thepun.data.queue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

import org.jctools.queues.MpmcArrayQueue;
import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.SpmcArrayQueue;
import org.jctools.queues.atomic.MpmcAtomicArrayQueue;
import org.openjdk.jmh.annotations.*;


@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 10, batchSize = 1)
@Measurement(iterations = 10, batchSize = 1)
@Fork(jvmArgs = {"-verbose:gc", "-XX:+PrintGCDetails", "-server", "-XX:+UseSerialGC", "-Xmn8000M", "-Xms10000M", "-Xmx10000M"})
public class MultiThreadDemultiplexerBenchmark {

    @Param({"16", "8", "4", "2"})
    private int cpu;

    private Long[] values;

    @Setup(Level.Iteration)
    public void prepareValues() {
        values = new Long[1_000_000];
        for (int l = 0; l < 1_000_000; l++) {
            values[l] = new Long(l);
        }
    }

    @TearDown(Level.Iteration)
    public void clearValues() throws InterruptedException {
        values = null;
    }

    @Benchmark
    public long ringBufferRouter() throws InterruptedException {
        RingBufferRouter<Long> queue = new RingBufferRouter<>(10000);

        QueueHead<Long>[] queueHeads = new QueueHead[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueHeads[i] = queue.createConsumer();
        }

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue.createProducer(), values, 100_000_000);
    }

    @Benchmark
    public long greedyRingBufferRouter() throws InterruptedException {
        GreedyRingBufferRouter<Long> queue = new GreedyRingBufferRouter<>(10000);

        QueueHead<Long>[] queueHeads = new QueueHead[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueHeads[i] = queue.createConsumer();
        }

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue.createProducer(), values, 100_000_000);
    }

    @Benchmark
    public long ringBufferDemultiplexer() throws InterruptedException {
        RingBufferDemultiplexer<Long> queue = new RingBufferDemultiplexer<>(10000);

        QueueHead<Long>[] queueHeads = new QueueHead[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueHeads[i] = queue.createConsumer();
        }

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue, values, 100_000_000);
    }

    @Benchmark
    public long stealingLinkedChunk() throws InterruptedException {
        StealingLinkedChunkDemultiplexer<Long> queue = new StealingLinkedChunkDemultiplexer<>();

        QueueHead<Long>[] queueHeads = new QueueHead[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueHeads[i] = queue.createConsumer();
        }

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue, values, 100_000_000);
    }

    @Benchmark
    public long arrayBlockingQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new ArrayBlockingQueue<Long>(10000));

        QueueHead<Long>[] queueHeads = new QueueHead[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueHeads[i] = queue;
        }

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue, values, 100_000_000);
    }

    @Benchmark
    public long linkedBlockingQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new LinkedBlockingQueue<>());

        QueueHead<Long>[] queueHeads = new QueueHead[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueHeads[i] = queue;
        }

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue, values, 100_000_000);
    }

    @Benchmark
    public long concurrentLinkedQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new ConcurrentLinkedQueue<>());

        QueueHead<Long>[] queueHeads = new QueueHead[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueHeads[i] = queue;
        }

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue, values, 100_000_000);
    }

    @Benchmark
    public long linkedTransferQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new LinkedTransferQueue<>());

        QueueHead<Long>[] queueHeads = new QueueHead[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueHeads[i] = queue;
        }

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue, values, 100_000_000);
    }

    @Benchmark
    public long spmcArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new SpmcArrayQueue<>(10000));

        QueueHead<Long>[] queueHeads = new QueueHead[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueHeads[i] = queue;
        }

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue, values, 100_000_000);
    }

    @Benchmark
    public long mpmcArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new MpmcArrayQueue<>(10000));

        QueueHead<Long>[] queueHeads = new QueueHead[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueHeads[i] = queue;
        }

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue, values, 100_000_000);
    }

    @Benchmark
    public long mpmcAtomicArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new MpmcAtomicArrayQueue<>(10000));

        QueueHead<Long>[] queueHeads = new QueueHead[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueHeads[i] = queue;
        }

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue, values, 100_000_000);
    }

    /*public static void main(String[] args) throws InterruptedException {
       FourThreadDemultiplexerBenchmark benchmark = new FourThreadDemultiplexerBenchmark();

        while (true) {
            benchmark.prepareValues();
            benchmark.greedyRingBufferRouter();
            System.out.println("next");
        }
    }*/
}
