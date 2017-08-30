package org.thepun.concurrency.queue;

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
public class SimpleQueuesBenchmark {

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
        return BenchmarkCases.singlewProducerAndSingleConsumer(queue, queue, values);
    }

    @Benchmark
    public long arrayBridge() throws InterruptedException {
        ArrayBridge<Long> queue = new ArrayBridge<>(10000);
        return BenchmarkCases.singlewProducerAndSingleConsumer(queue, queue, values);
    }

    @Benchmark
    public long arrayQueue() throws InterruptedException {
        ArrayQueue<Long> queue = new ArrayQueue<>(10000);
        return BenchmarkCases.singlewProducerAndSingleConsumer(queue, queue, values);
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
