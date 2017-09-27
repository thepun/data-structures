package org.thepun.data.queue;

import org.jctools.queues.MpmcArrayQueue;
import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.atomic.MpmcAtomicArrayQueue;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;


@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 1, batchSize = 1)
@Measurement(iterations = 1, batchSize = 1)
@Fork(jvmArgs = {/*"-verbose:gc",*/ "-XX:+PrintGCDetails", "-server", "-XX:+UseSerialGC", "-Xmn8000M", "-Xms10000M", "-Xmx10000M"})
public class MultiThreadMultiplexerBenchmark {

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

        QueueTail<Long>[] queueTails = new QueueTail[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueTails[i] = queue.createProducer();
        }

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue.createConsumer(), queueTails, values, 100_000_000);
    }

    @Benchmark
    public long greedyRingBufferRouter() throws InterruptedException {
        GreedyRingBufferRouter<Long> queue = new GreedyRingBufferRouter<>(10000);

        QueueTail<Long>[] queueTails = new QueueTail[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueTails[i] = queue.createProducer();
        }

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue.createConsumer(), queueTails, values, 100_000_000);
    }

    @Benchmark
    public long greedyRingBufferMultiplexer() throws InterruptedException {
        GreedyRingBufferMultiplexer<Long> queue = new GreedyRingBufferMultiplexer<>(10000);

        QueueTail<Long>[] queueTails = new QueueTail[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueTails[i] = queue.createProducer();
        }

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long unfairLinkedChunk() throws InterruptedException {
        UnfairLinkedChunkMultiplexer<Long> queue = new UnfairLinkedChunkMultiplexer<>();

        QueueTail<Long>[] queueTails = new QueueTail[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueTails[i] = queue.createProducer();
        }

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long atomicPool() throws InterruptedException {
        AtomicPoolRouter<Long> queue = new AtomicPoolRouter<>(10000);

        QueueTail<Long>[] queueTails = new QueueTail[3];
        queueTails[0] = queue.createProducer();
        queueTails[1] = queue.createProducer();
        queueTails[2] = queue.createProducer();

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue.createConsumer(), queueTails, values, 100_000_000);
    }

    @Benchmark
    public long atomicBuffer() throws InterruptedException {
        AtomicBufferRouter<Long> queue = new AtomicBufferRouter<>(10000);

        QueueTail<Long>[] queueTails = new QueueTail[3];
        queueTails[0] = queue.createProducer();
        queueTails[1] = queue.createProducer();
        queueTails[2] = queue.createProducer();

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue.createConsumer(), queueTails, values, 100_000_000);
    }

    @Benchmark
    public long arrayBlockingQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new ArrayBlockingQueue<Long>(1000));

        QueueTail<Long>[] queueTails = new QueueTail[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long linkedBlockingQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new LinkedBlockingQueue<Long>(1000));

        QueueTail<Long>[] queueTails = new QueueTail[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long concurrentLinkedQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new ConcurrentLinkedQueue<>());

        QueueTail<Long>[] queueTails = new QueueTail[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long linkedTransferQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new LinkedTransferQueue<Long>());

        QueueTail<Long>[] queueTails = new QueueTail[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long mpscArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new MpscArrayQueue<>(1000));

        QueueTail<Long>[] queueTails = new QueueTail[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long mpmcArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new MpmcArrayQueue<>(1000));

        QueueTail<Long>[] queueTails = new QueueTail[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long mpmcAtomicArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new MpmcAtomicArrayQueue<>(1000));

        QueueTail<Long>[] queueTails = new QueueTail[cpu - 1];
        for (int i = 0; i < cpu - 1; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

   /*public static void main(String[] args) throws InterruptedException {
        FourThreadBenchmark benchmark = new FourThreadBenchmark();

        while (true) {
            benchmark.prepareValues();
            benchmark.ringBufferRouterWithXADDMultiplexer();
            System.out.println("next");
        }
    }*/
}


/*
 AMD Ryzen 7 1700
    8 cores (16 threads)

Benchmark                                                    (cpu)  Mode  Cnt   Score    Error  Units
MultiThreadMultiplexerBenchmark.arrayBlockingQueue              16  avgt   10  96.997 ± 25.998   s/op
MultiThreadMultiplexerBenchmark.atomicBuffer                    16  avgt   10  19.356 ±  0.606   s/op
MultiThreadMultiplexerBenchmark.atomicPool                      16  avgt   10   5.245 ±  0.402   s/op
MultiThreadMultiplexerBenchmark.concurrentLinkedQueue           16  avgt   10  34.381 ±  2.731   s/op
MultiThreadMultiplexerBenchmark.greedyRingBufferMultiplexer     16  avgt   10   6.348 ±  0.743   s/op
MultiThreadMultiplexerBenchmark.greedyRingBufferRouter          16  avgt   10  20.749 ±  1.444   s/op
MultiThreadMultiplexerBenchmark.linkedBlockingQueue             16  avgt   10  68.094 ± 16.287   s/op
MultiThreadMultiplexerBenchmark.linkedTransferQueue             16  avgt   10  29.858 ±  0.497   s/op
MultiThreadMultiplexerBenchmark.mpmcArrayQueue                  16  avgt   10  28.251 ±  6.460   s/op
MultiThreadMultiplexerBenchmark.mpmcAtomicArrayQueue            16  avgt   10  36.144 ±  1.729   s/op
MultiThreadMultiplexerBenchmark.mpscArrayQueue                  16  avgt   10  11.208 ±  0.384   s/op
MultiThreadMultiplexerBenchmark.ringBufferRouter                16  avgt   10  26.642 ±  7.979   s/op
MultiThreadMultiplexerBenchmark.unfairLinkedChunk               16  avgt   10   0.851 ±  0.085   s/op

 */
