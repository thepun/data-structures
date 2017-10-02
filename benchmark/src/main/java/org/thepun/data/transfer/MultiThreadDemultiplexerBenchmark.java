package org.thepun.data.transfer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

import org.jctools.queues.MpmcArrayQueue;
import org.jctools.queues.SpmcArrayQueue;
import org.jctools.queues.atomic.MpmcAtomicArrayQueue;
import org.openjdk.jmh.annotations.*;


@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 1, batchSize = 1)
@Measurement(iterations = 1, batchSize = 1)
@Fork(jvmArgs = {"-verbose:gc", "-XX:+PrintGCDetails", "-server", "-XX:+UseSerialGC", "-Xmn8000M", "-Xms10000M", "-Xmx10000M"})
public class MultiThreadDemultiplexerBenchmark {

    @Param({"64", "32", "16", "8", "4", "2"})
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
    public long atomicPool() throws InterruptedException {
        AtomicPoolRouter<Long> queue = new AtomicPoolRouter<>(10000);

        QueueHead<Long>[] queueHeads = new QueueHead[3];
        queueHeads[0] = queue.createConsumer();
        queueHeads[1] = queue.createConsumer();
        queueHeads[2] = queue.createConsumer();

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue.createProducer(), values, 100_000_000);
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



/*
    ----------------------------------------------------------

    AMD Ryzen 7 1700
    8 cores (16 threads)
                             (cpu)  Mode  Cnt   Score    Error

    arrayBlockingQueue          16  avgt   10 123.283 ± 68.813
    atomicBuffer                16  avgt   10  17.765 ±  0.435
    atomicPool                  16  avgt   10   5.720 ±  0.052
    concurrentLinkedQueue       16  avgt   10  78.068 ± 18.459
    greedyRingBufferRouter      16  avgt   10  35.656 ±  3.018
    linkedBlockingQueue         16  avgt   10  20.767 ±  4.203
    linkedTransferQueue         16  avgt   10  77.447 ±  4.941
    mpmcArrayQueue              16  avgt   10  26.809 ±  5.234
    mpmcAtomicArrayQueue        16  avgt   10  32.284 ±  6.042
    ringBufferDemultiplexer     16  avgt   10  10.028 ±  0.332
    ringBufferRouter            16  avgt   10  31.586 ±  1.455
    spmcArrayQueue              16  avgt   10   9.600 ±  0.307
    stealingLinkedChunk         16  avgt   10   4.482 ±  1.591

    arrayBlockingQueue           8  avgt   10  71.160 ± 30.654
    atomicBuffer                 8  avgt   10  17.811 ±  0.437
    atomicPool                   8  avgt   10   5.714 ±  0.044
    concurrentLinkedQueue        8  avgt   10  61.006 ± 20.249
    greedyRingBufferRouter       8  avgt   10  16.158 ± 10.019
    linkedBlockingQueue          8  avgt   10  23.870 ±  3.940
    linkedTransferQueue          8  avgt   10  62.746 ±  1.611
    mpmcArrayQueue               8  avgt   10  20.873 ±  1.203
    mpmcAtomicArrayQueue         8  avgt   10  20.507 ±  0.920
    ringBufferDemultiplexer      8  avgt   10   7.111 ±  0.207
    ringBufferRouter             8  avgt   10  10.994 ±  8.576
    spmcArrayQueue               8  avgt   10   9.792 ±  0.884
    stealingLinkedChunk          8  avgt   10   5.151 ±  0.267

    arrayBlockingQueue           4  avgt   10  25.889 ±  1.852
    atomicBuffer                 4  avgt   10  17.716 ±  0.408
    atomicPool                   4  avgt   10   5.703 ±  0.091
    concurrentLinkedQueue        4  avgt   10  43.138 ±  1.988
    greedyRingBufferRouter       4  avgt   10   8.249 ±  1.485
    linkedBlockingQueue          4  avgt   10  16.873 ±  1.317
    linkedTransferQueue          4  avgt   10  47.442 ±  1.112
    mpmcArrayQueue               4  avgt   10  22.070 ±  0.669
    mpmcAtomicArrayQueue         4  avgt   10  22.311 ±  0.346
    ringBufferDemultiplexer      4  avgt   10   4.794 ±  0.300
    ringBufferRouter             4  avgt   10  11.853 ±  0.482
    spmcArrayQueue               4  avgt   10  12.756 ±  0.759
    stealingLinkedChunk          4  avgt   10   5.418 ±  0.352

    arrayBlockingQueue           2  avgt   10  38.693 ±  7.474
    atomicBuffer                 2  avgt   10  17.778 ±  0.423
    atomicPool                   2  avgt   10   5.706 ±  0.060
    concurrentLinkedQueue        2  avgt   10  23.028 ±  3.616
    greedyRingBufferRouter       2  avgt   10  17.491 ±  1.084
    linkedBlockingQueue          2  avgt   10  54.103 ± 14.311
    linkedTransferQueue          2  avgt   10  15.012 ±  2.197
    mpmcArrayQueue               2  avgt   10  12.828 ±  2.143
    mpmcAtomicArrayQueue         2  avgt   10  15.475 ±  4.827
    ringBufferDemultiplexer      2  avgt   10   2.122 ±  0.029
    ringBufferRouter             2  avgt   10  25.307 ±  6.096
    spmcArrayQueue               2  avgt   10  12.803 ±  6.913
    stealingLinkedChunk          2  avgt   10   4.631 ±  5.932

    ----------------------------------------------------------
*/


/*

Intel(R) Core(TM) i7 CPU         870  @ 2.93GHz

Benchmark                                                  (cpu)  Mode  Cnt   Score   Error  Units

MultiThreadDemultiplexerBenchmark.arrayBlockingQueue           8  avgt   10  58.767 ± 7.801   s/op
MultiThreadDemultiplexerBenchmark.atomicBuffer                 8  avgt   10   6.757 ± 0.136   s/op
MultiThreadDemultiplexerBenchmark.atomicPool                   8  avgt   10   3.554 ± 0.169   s/op
MultiThreadDemultiplexerBenchmark.concurrentLinkedQueue        8  avgt   10  25.197 ± 0.382   s/op
MultiThreadDemultiplexerBenchmark.greedyRingBufferRouter       8  avgt   10  16.134 ± 1.159   s/op
MultiThreadDemultiplexerBenchmark.linkedBlockingQueue          8  avgt   10  19.184 ± 1.193   s/op
MultiThreadDemultiplexerBenchmark.linkedTransferQueue          8  avgt   10  28.728 ± 1.559   s/op
MultiThreadDemultiplexerBenchmark.mpmcArrayQueue               8  avgt   10  10.452 ± 0.358   s/op
MultiThreadDemultiplexerBenchmark.mpmcAtomicArrayQueue         8  avgt   10  10.331 ± 0.393   s/op
MultiThreadDemultiplexerBenchmark.ringBufferDemultiplexer      8  avgt   10   3.487 ± 0.088   s/op
MultiThreadDemultiplexerBenchmark.ringBufferRouter             8  avgt   10  14.040 ± 1.753   s/op
MultiThreadDemultiplexerBenchmark.spmcArrayQueue               8  avgt   10   3.630 ± 0.070   s/op
MultiThreadDemultiplexerBenchmark.stealingLinkedChunk          8  avgt   10   2.874 ± 0.469   s/op

MultiThreadDemultiplexerBenchmark.arrayBlockingQueue           4  avgt   10  28.156 ± 4.187   s/op
MultiThreadDemultiplexerBenchmark.atomicBuffer                 4  avgt   10   6.640 ± 0.802   s/op
MultiThreadDemultiplexerBenchmark.atomicPool                   4  avgt   10   3.558 ± 0.220   s/op
MultiThreadDemultiplexerBenchmark.concurrentLinkedQueue        4  avgt   10  19.823 ± 3.880   s/op
MultiThreadDemultiplexerBenchmark.greedyRingBufferRouter       4  avgt   10   4.625 ± 1.897   s/op
MultiThreadDemultiplexerBenchmark.linkedBlockingQueue          4  avgt   10  15.311 ± 1.357   s/op
MultiThreadDemultiplexerBenchmark.linkedTransferQueue          4  avgt   10  26.655 ± 2.535   s/op
MultiThreadDemultiplexerBenchmark.mpmcArrayQueue               4  avgt   10  11.332 ± 0.603   s/op
MultiThreadDemultiplexerBenchmark.mpmcAtomicArrayQueue         4  avgt   10  10.568 ± 0.943   s/op
MultiThreadDemultiplexerBenchmark.ringBufferDemultiplexer      4  avgt   10   3.523 ± 0.082   s/op
MultiThreadDemultiplexerBenchmark.ringBufferRouter             4  avgt   10   4.930 ± 1.358   s/op
MultiThreadDemultiplexerBenchmark.spmcArrayQueue               4  avgt   10   5.291 ± 0.288   s/op
MultiThreadDemultiplexerBenchmark.stealingLinkedChunk          4  avgt   10   2.933 ± 0.110   s/op

MultiThreadDemultiplexerBenchmark.arrayBlockingQueue           2  avgt   10  25.759 ±  1.307   s/op
MultiThreadDemultiplexerBenchmark.atomicBuffer                 2  avgt   10   6.741 ±  0.118   s/op
MultiThreadDemultiplexerBenchmark.atomicPool                   2  avgt   10   3.586 ±  0.129   s/op
MultiThreadDemultiplexerBenchmark.concurrentLinkedQueue        2  avgt   10  10.427 ±  4.389   s/op
MultiThreadDemultiplexerBenchmark.greedyRingBufferRouter       2  avgt   10   6.780 ±  1.228   s/op
MultiThreadDemultiplexerBenchmark.linkedBlockingQueue          2  avgt   10  33.158 ± 13.756   s/op
MultiThreadDemultiplexerBenchmark.linkedTransferQueue          2  avgt   10  12.475 ±  0.621   s/op
MultiThreadDemultiplexerBenchmark.mpmcArrayQueue               2  avgt   10   8.192 ±  1.606   s/op
MultiThreadDemultiplexerBenchmark.mpmcAtomicArrayQueue         2  avgt   10   7.443 ±  0.892   s/op
MultiThreadDemultiplexerBenchmark.ringBufferDemultiplexer      2  avgt   10   2.068 ±  0.071   s/op
MultiThreadDemultiplexerBenchmark.ringBufferRouter             2  avgt   10   5.475 ±  2.927   s/op
MultiThreadDemultiplexerBenchmark.spmcArrayQueue               2  avgt   10   5.786 ±  2.117   s/op
MultiThreadDemultiplexerBenchmark.stealingLinkedChunk          2  avgt   10   2.287 ±  0.177   s/op

 */