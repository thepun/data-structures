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

MultiThreadMultiplexerBenchmark.arrayBlockingQueue               8  avgt   10  60.379 ± 10.310   s/op
MultiThreadMultiplexerBenchmark.atomicBuffer                     8  avgt   10  19.419 ±  0.739   s/op
MultiThreadMultiplexerBenchmark.atomicPool                       8  avgt   10   5.260 ±  0.419   s/op
MultiThreadMultiplexerBenchmark.concurrentLinkedQueue            8  avgt   10  35.074 ±  1.001   s/op
MultiThreadMultiplexerBenchmark.greedyRingBufferMultiplexer      8  avgt   10   7.396 ±  0.374   s/op
MultiThreadMultiplexerBenchmark.greedyRingBufferRouter           8  avgt   10   7.345 ±  0.185   s/op
MultiThreadMultiplexerBenchmark.linkedBlockingQueue              8  avgt   10  57.676 ± 16.006   s/op
MultiThreadMultiplexerBenchmark.linkedTransferQueue              8  avgt   10  30.613 ±  0.446   s/op
MultiThreadMultiplexerBenchmark.mpmcArrayQueue                   8  avgt   10  22.608 ±  0.747   s/op
MultiThreadMultiplexerBenchmark.mpmcAtomicArrayQueue             8  avgt   10  23.835 ±  0.613   s/op
MultiThreadMultiplexerBenchmark.mpscArrayQueue                   8  avgt   10   9.969 ±  0.398   s/op
MultiThreadMultiplexerBenchmark.ringBufferRouter                 8  avgt   10   8.081 ±  0.751   s/op
MultiThreadMultiplexerBenchmark.unfairLinkedChunk                8  avgt   10   1.188 ±  0.211   s/op

MultiThreadMultiplexerBenchmark.arrayBlockingQueue               4  avgt   10  16.417 ±  2.129   s/op
MultiThreadMultiplexerBenchmark.atomicBuffer                     4  avgt   10  19.605 ±  0.571   s/op
MultiThreadMultiplexerBenchmark.atomicPool                       4  avgt   10   5.250 ±  0.430   s/op
MultiThreadMultiplexerBenchmark.concurrentLinkedQueue            4  avgt   10  28.754 ±  0.314   s/op
MultiThreadMultiplexerBenchmark.greedyRingBufferMultiplexer      4  avgt   10  14.393 ±  0.357   s/op
MultiThreadMultiplexerBenchmark.greedyRingBufferRouter           4  avgt   10  14.291 ±  0.424   s/op
MultiThreadMultiplexerBenchmark.linkedBlockingQueue              4  avgt   10  62.378 ± 31.809   s/op
MultiThreadMultiplexerBenchmark.linkedTransferQueue              4  avgt   10  27.570 ±  0.751   s/op
MultiThreadMultiplexerBenchmark.mpmcArrayQueue                   4  avgt   10  22.255 ±  0.677   s/op
MultiThreadMultiplexerBenchmark.mpmcAtomicArrayQueue             4  avgt   10  22.589 ±  0.780   s/op
MultiThreadMultiplexerBenchmark.mpscArrayQueue                   4  avgt   10  12.041 ±  0.528   s/op
MultiThreadMultiplexerBenchmark.ringBufferRouter                 4  avgt   10  13.281 ±  0.565   s/op
MultiThreadMultiplexerBenchmark.unfairLinkedChunk                4  avgt   10   1.405 ±  0.297   s/op

 */


/*

Intel(R) Core(TM) i7 CPU         870  @ 2.93GHz

Benchmark                                                    (cpu)  Mode  Cnt   Score    Error  Units

MultiThreadMultiplexerBenchmark.arrayBlockingQueue               8  avgt   10  46.913 ?  9.272   s/op
MultiThreadMultiplexerBenchmark.atomicBuffer                     8  avgt   10   8.077 ?  0.345   s/op
MultiThreadMultiplexerBenchmark.atomicPool                       8  avgt   10   3.271 ?  0.389   s/op
MultiThreadMultiplexerBenchmark.concurrentLinkedQueue            8  avgt   10  13.040 ? 10.744   s/op
MultiThreadMultiplexerBenchmark.greedyRingBufferMultiplexer      8  avgt   10   4.729 ?  9.493   s/op
MultiThreadMultiplexerBenchmark.greedyRingBufferRouter           8  avgt   10  11.628 ?  2.852   s/op
MultiThreadMultiplexerBenchmark.linkedBlockingQueue              8  avgt   10  31.897 ?  4.061   s/op
MultiThreadMultiplexerBenchmark.linkedTransferQueue              8  avgt   10  13.538 ?  4.820   s/op
MultiThreadMultiplexerBenchmark.mpmcArrayQueue                   8  avgt   10  11.582 ?  0.512   s/op
MultiThreadMultiplexerBenchmark.mpmcAtomicArrayQueue             8  avgt   10  11.417 ?  0.549   s/op
MultiThreadMultiplexerBenchmark.mpscArrayQueue                   8  avgt   10   3.981 ?  0.154   s/op
MultiThreadMultiplexerBenchmark.ringBufferRouter                 8  avgt   10  14.596 ?  2.011   s/op
MultiThreadMultiplexerBenchmark.unfairLinkedChunk                8  avgt   10   1.894 ?  0.047   s/op

 */