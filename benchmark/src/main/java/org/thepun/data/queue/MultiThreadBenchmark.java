package org.thepun.data.queue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

import org.jctools.queues.MpmcArrayQueue;
import org.jctools.queues.atomic.MpmcAtomicArrayQueue;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;


@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 1, batchSize = 1)
@Measurement(iterations = 1, batchSize = 1)
@Fork(jvmArgs = {/*"-verbose:gc",*/ "-XX:+PrintGCDetails", "-server", "-XX:+UseSerialGC", "-Xmn8000M", "-Xms10000M", "-Xmx10000M"})
public class MultiThreadBenchmark {

    @Param({"64", "32", "16", "8", "4", "2"})
    //@Param({"4"})
    private int cpu;

    private int halfCpu;

    private Long[] values;

    @Setup(Level.Iteration)
    public void prepareValues() {
        halfCpu = cpu / 2;

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

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue.createConsumer();
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue.createProducer();
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long greedyRingBufferRouter() throws InterruptedException {
        GreedyRingBufferRouter<Long> queue = new GreedyRingBufferRouter<>(10000);

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue.createConsumer();
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue.createProducer();
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long atomicPool() throws InterruptedException {
        AtomicPoolRouter<Long> queue = new AtomicPoolRouter<>(10000);

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue.createConsumer();
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue.createProducer();
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long arraydBlockingQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new ArrayBlockingQueue<Long>(10000));

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue;
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long linkedTransferQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new LinkedTransferQueue<>());

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue;
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long mpmcArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new MpmcArrayQueue<>(10000));

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue;
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long mpmcAtomicArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new MpmcAtomicArrayQueue<>(10000));

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue;
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }

   /*public static void main(String[] args) throws InterruptedException {
        MultiThreadBenchmark benchmark = new MultiThreadBenchmark();

        while (true) {
            benchmark.prepareValues();
            benchmark.cpu = 4;
            benchmark.halfCpu = 2;
            benchmark.atomicBuffer();
            System.out.println("next");
        }
    }*/
}



/*
    --------------------------------------------------------

    AMD Ryzen 7 1700
    8 cores (16 threads)
                            (cpu)  Mode  Cnt   Score   Error

    atomicBuffer               16  avgt   10  23.708 ± 1.464
    atomicPool                 16  avgt   10   6.289 ± 0.509
    greedyRingBufferRouter     16  avgt   10   6.521 ± 1.072
    linkedTransferQueue        16  avgt   10  39.674 ± 0.811
    mpmcArrayQueue             16  avgt   10  19.320 ± 0.630
    mpmcAtomicArrayQueue       16  avgt   10  18.628 ± 1.564
    ringBufferRouter           16  avgt   10  11.329 ± 3.150

    atomicBuffer                8  avgt   10  25.316 ± 0.743
    atomicPool                  8  avgt   10   5.471 ± 1.038
    greedyRingBufferRouter      8  avgt   10  11.544 ± 0.386
    linkedTransferQueue         8  avgt   10  42.163 ± 0.824
    mpmcArrayQueue              8  avgt   10  18.395 ± 0.924
    mpmcAtomicArrayQueue        8  avgt   10  18.388 ± 0.914
    ringBufferRouter            8  avgt   10   9.906 ± 1.014

    atomicBuffer                4  avgt   10  22.671 ± 1.030
    atomicPool                  4  avgt   10   5.470 ± 0.676
    greedyRingBufferRouter      4  avgt   10  22.397 ± 0.265
    linkedTransferQueue         4  avgt   10  41.132 ± 2.921
    mpmcArrayQueue              4  avgt   10  19.394 ± 3.793
    mpmcAtomicArrayQueue        4  avgt   10  17.013 ± 1.880
    ringBufferRouter            4  avgt   10  21.277 ± 0.391

    atomicBuffer                2  avgt   10  14.806 ± 2.365
    atomicPool                  2  avgt   10   3.539 ± 0.423
    greedyRingBufferRouter      2  avgt   10  17.833 ± 6.230
    linkedTransferQueue         2  avgt   10  16.049 ± 1.429
    mpmcArrayQueue              2  avgt   10  13.883 ± 2.061
    mpmcAtomicArrayQueue        2  avgt   10  18.377 ± 3.988
    ringBufferRouter            2  avgt   10  24.669 ± 6.833

    --------------------------------------------------------
*/


/*
    --------------------------------------------------------

    Intel(R) Core(TM) i7 870
    4 cores (8 threads)
                             (cpu) Mode  Cnt   Score   Error

    arraydBlockingQueue         8  avgt   10  14.790 ± 2.051
    atomicBuffer                8  avgt   10   7.795 ± 0.760
    atomicPool                  8  avgt   10   3.107 ± 0.337
    greedyRingBufferRouter      8  avgt   10   6.673 ± 1.132
    linkedTransferQueue         8  avgt   10  16.057 ± 7.634
    mpmcArrayQueue              8  avgt   10  10.467 ± 1.505
    mpmcAtomicArrayQueue        8  avgt   10  11.389 ± 1.081
    ringBufferRouter            8  avgt   10   7.674 ± 1.387

    arraydBlockingQueue         4  avgt   10  12.820 ± 2.836
    atomicBuffer                4  avgt   10   8.401 ± 0.765
    atomicPool                  4  avgt   10   3.508 ± 0.511
    greedyRingBufferRouter      4  avgt   10   4.596 ± 1.971
    linkedTransferQueue         4  avgt   10  17.026 ± 1.458
    mpmcArrayQueue              4  avgt   10   8.062 ± 0.889
    mpmcAtomicArrayQueue        4  avgt   10   7.932 ± 1.134
    ringBufferRouter            4  avgt   10   7.564 ± 0.453

    arraydBlockingQueue         2  avgt   10  25.552 ± 1.703
    atomicBuffer                2  avgt   10   3.537 ± 2.214
    atomicPool                  2  avgt   10   2.271 ± 0.043
    greedyRingBufferRouter      2  avgt   10   7.926 ± 2.973
    linkedTransferQueue         2  avgt   10  13.053 ± 7.174
    mpmcArrayQueue              2  avgt   10   7.103 ± 2.914
    mpmcAtomicArrayQueue        2  avgt   10   7.436 ± 1.232
    ringBufferRouter            2  avgt   10   4.529 ± 3.053

    --------------------------------------------------------
 */