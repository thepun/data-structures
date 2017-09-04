package org.thepun.data.queue;

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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;


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
        LinkedBridge<Long> queue = new LinkedBridge<>();
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
    public long ringBufferRouterWithXADD() throws InterruptedException {
        RingBufferRouterWithXADD<Long> queue = new RingBufferRouterWithXADD<>(1000);
        return BenchmarkCases.singleProducerAndSingleConsumer(queue.createConsumer(), queue.createProducer(), values, 100_000_000);
    }

    @Benchmark
    public long roundRobinMultiplexer() throws InterruptedException {
        RoundRobinLinkedMultiplexer<Long> queue = new RoundRobinLinkedMultiplexer<>();
        return BenchmarkCases.singleProducerAndSingleConsumer(queue, queue.createProducer(), values, 100_000_000);
    }

    @Benchmark
    public long roundRobinDemultiplexer() throws InterruptedException {
        RoundRobinLinkedDemultiplexer<Long> queue = new RoundRobinLinkedDemultiplexer<>();
        return BenchmarkCases.singleProducerAndSingleConsumer(queue.createConsumer(), queue, values, 100_000_000);
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

    /*public static void main(String[] args) throws InterruptedException {
        while (true) {
            SimpleQueuesBenchmark benchmark = new SimpleQueuesBenchmark();
            benchmark.prepareValues();
            benchmark.arrayQueue();

            System.out.println("next");
        }
    }*/
}
