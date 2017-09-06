package org.thepun.data.queue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

import org.jctools.queues.SpscArrayQueue;
import org.jctools.queues.SpscChunkedArrayQueue;
import org.jctools.queues.SpscGrowableArrayQueue;
import org.jctools.queues.SpscLinkedQueue;
import org.jctools.queues.SpscUnboundedArrayQueue;
import org.jctools.queues.atomic.SpscAtomicArrayQueue;
import org.jctools.queues.atomic.SpscLinkedAtomicQueue;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;


@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 10, batchSize = 1)
@Measurement(iterations = 10, batchSize = 1)
@Fork(jvmArgs = {/*"-verbose:gc",*/ "-XX:+PrintGCDetails", "-server", "-XX:+UseSerialGC", "-Xmn8000M", "-Xms10000M", "-Xmx10000M"})
public class TwoThreadBenchmark {

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
    public long linkedBridge() throws InterruptedException {
        LinkedChunkBridge<Long> queue = new LinkedChunkBridge<>();
        return BenchmarkCases.singleProducerAndSingleConsumer(queue, queue, values, 100_000_000);
    }

    @Benchmark
    public long ringBufferBridge() throws InterruptedException {
        RingBufferBridge<Long> queue = new RingBufferBridge<>(1000);
        return BenchmarkCases.singleProducerAndSingleConsumer(queue, queue, values, 100_000_000);
    }

    @Benchmark
    public long ringBufferRouter() throws InterruptedException {
        RingBufferRouter<Long> queue = new RingBufferRouter<>(1000);
        return BenchmarkCases.singleProducerAndSingleConsumer(queue.createConsumer(), queue.createProducer(), values, 100_000_000);
    }

    @Benchmark
    public long ringBufferDemultiplexer() throws InterruptedException {
        RingBufferDemultiplexer<Long> queue = new RingBufferDemultiplexer<>(1000);
        return BenchmarkCases.singleProducerAndSingleConsumer(queue.createConsumer(), queue, values, 100_000_000);
    }

    @Benchmark
    public long roundRobinMultiplexer() throws InterruptedException {
        UnfairLinkedChunkMultiplexer<Long> queue = new UnfairLinkedChunkMultiplexer<>();
        return BenchmarkCases.singleProducerAndSingleConsumer(queue, queue.createProducer(), values, 100_000_000);
    }

    @Benchmark
    public long arrayBlockingQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new ArrayBlockingQueue<Long>(1000));
        return BenchmarkCases.singleProducerAndSingleConsumer(queue, queue, values, 100_000_000);
    }

    @Benchmark
    public long linkedBlockingQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new LinkedBlockingQueue<>());
        return BenchmarkCases.singleProducerAndSingleConsumer(queue, queue, values, 100_000_000);
    }

    @Benchmark
    public long linkedTransferQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new LinkedTransferQueue<>());
        return BenchmarkCases.singleProducerAndSingleConsumer(queue, queue, values, 100_000_000);
    }

    @Benchmark
    public long spscArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new SpscArrayQueue<>(1000));
        return BenchmarkCases.singleProducerAndSingleConsumer(queue, queue, values, 100_000_000);
    }

    @Benchmark
    public long spscChunkedArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new SpscChunkedArrayQueue<>(1000));
        return BenchmarkCases.singleProducerAndSingleConsumer(queue, queue, values, 100_000_000);
    }

    @Benchmark
    public long spscGrowableArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new SpscGrowableArrayQueue<>(1000));
        return BenchmarkCases.singleProducerAndSingleConsumer(queue, queue, values, 100_000_000);
    }

    @Benchmark
    public long spscUnboundedArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new SpscUnboundedArrayQueue<>(1000));
        return BenchmarkCases.singleProducerAndSingleConsumer(queue, queue, values, 100_000_000);
    }

    @Benchmark
    public long spscAtomicArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new SpscAtomicArrayQueue<>(1000));
        return BenchmarkCases.singleProducerAndSingleConsumer(queue, queue, values, 100_000_000);
    }

    @Benchmark
    public long spscAtomicLinkedQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new SpscLinkedAtomicQueue<>());
        return BenchmarkCases.singleProducerAndSingleConsumer(queue, queue, values, 100_000_000);
    }

    @Benchmark
    public long spscLinkedQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new SpscLinkedQueue<>());
        return BenchmarkCases.singleProducerAndSingleConsumer(queue, queue, values, 100_000_000);
    }

    public static void main(String[] args) throws InterruptedException {
        while (true) {
            TwoThreadBenchmark benchmark = new TwoThreadBenchmark();
            benchmark.prepareValues();
            benchmark.ringBufferDemultiplexer();

            System.out.println("next");
        }
    }
}
