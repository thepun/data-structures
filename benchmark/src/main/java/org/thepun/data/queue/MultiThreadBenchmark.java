package org.thepun.data.queue;

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

    @Param({"16", "8", "4", "2"})
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
    public long atomicBuffer() throws InterruptedException {
        AtomicBufferRouter<Long> queue = new AtomicBufferRouter<>(10000);

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

    /*@Benchmark
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
    }*/

    /*@Benchmark
    public long linkedBlockingQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new LinkedBlockingQueue<>());

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue;
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }*/

    /*@Benchmark
    public long concurrentLinkedQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new ConcurrentLinkedQueue<>());

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue;
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }*/

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
    ------------------------------------------------------------------------------------

    AMD Ryzen 7 1700
    8 cores (16 threads)

    Benchmark                                    (cpu)  Mode  Cnt   Score   Error  Units

    MultiThreadBenchmark.atomicBuffer               16  avgt   10  23.708 ± 1.464   s/op
    MultiThreadBenchmark.atomicPool                 16  avgt   10   6.289 ± 0.509   s/op
    MultiThreadBenchmark.greedyRingBufferRouter     16  avgt   10   6.521 ± 1.072   s/op
    MultiThreadBenchmark.linkedTransferQueue        16  avgt   10  39.674 ± 0.811   s/op
    MultiThreadBenchmark.mpmcArrayQueue             16  avgt   10  19.320 ± 0.630   s/op
    MultiThreadBenchmark.mpmcAtomicArrayQueue       16  avgt   10  18.628 ± 1.564   s/op
    MultiThreadBenchmark.ringBufferRouter           16  avgt   10  11.329 ± 3.150   s/op

    MultiThreadBenchmark.atomicBuffer                8  avgt   10  25.316 ± 0.743   s/op
    MultiThreadBenchmark.atomicPool                  8  avgt   10   5.471 ± 1.038   s/op
    MultiThreadBenchmark.greedyRingBufferRouter      8  avgt   10  11.544 ± 0.386   s/op
    MultiThreadBenchmark.linkedTransferQueue         8  avgt   10  42.163 ± 0.824   s/op
    MultiThreadBenchmark.mpmcArrayQueue              8  avgt   10  18.395 ± 0.924   s/op
    MultiThreadBenchmark.mpmcAtomicArrayQueue        8  avgt   10  18.388 ± 0.914   s/op
    MultiThreadBenchmark.ringBufferRouter            8  avgt   10   9.906 ± 1.014   s/op

    ------------------------------------------------------------------------------------
*/