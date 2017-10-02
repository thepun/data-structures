package org.thepun.data.transfer;

import java.util.Random;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 10, batchSize = 10)
@Measurement(iterations = 10, batchSize = 10)
public class SimdBenchmark {

    private static final int N = 10 * 1024 * 1024;


    private int[] arrayA;
    private int[] arrayB;
    private int[] arrayC;

    @Setup(Level.Iteration)
    public void prepareData() {
        Random random = new Random();

        arrayA = new int[N];
        arrayB = new int[N];
        arrayC = new int[N];

        for (int i = 0; i < N; i++) {
            arrayA[i] = random.nextInt()  + 7 * 2;
            arrayB[i] = random.nextInt() * 1000 - 99 ;
        }
    }

    @Benchmark
    @Fork(jvmArgs = {"-XX:+PrintGCDetails", "-server", "-XX:+UseSerialGC", "-Xmn8000M", "-Xms10000M", "-Xmx10000M", "-XX:+UseSuperWord", "-XX:+OptimizeFill"/*,
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintAssembly", "-XX:CompileCommand=compileonly,*SimdBenchmark"*/})
    public int[] withSimd() {
        for (int i = 0; i < N; i++) {
            arrayC[i] = arrayA[i] * arrayB[i];
        }
        return arrayC;
    }

    @Benchmark
    @Fork(jvmArgs = {"-XX:+PrintGCDetails", "-server", "-XX:+UseSerialGC", "-Xmn8000M", "-Xms10000M", "-Xmx10000M", "-XX:-UseSuperWord", "-XX:-OptimizeFill"/*,
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintAssembly", "-XX:CompileCommand=compileonly,*SimdBenchmark"*/})
    public int[] withoutSimd() {
        for (int i = 0; i < N; i++) {
            arrayC[i] = arrayA[i] * arrayB[i];
        }
        return arrayC;
    }

    @Benchmark
    @Fork(jvmArgs = {"-XX:+PrintGCDetails", "-server", "-XX:+UseSerialGC", "-Xmn8000M", "-Xms10000M", "-Xmx10000M", "-XX:+UseSuperWord", "-XX:+OptimizeFill"/*,
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintAssembly", "-XX:CompileCommand=compileonly,*SimdBenchmark"*/})
    public int[] bitWithSimd() {
        for (int i = 0; i < N; i++) {
            arrayC[i] = arrayB[i] & arrayA[i];
        }
        return arrayC;
    }

    @Benchmark
    @Fork(jvmArgs = {"-XX:+PrintGCDetails", "-server", "-XX:+UseSerialGC", "-Xmn8000M", "-Xms10000M", "-Xmx10000M", "-XX:-UseSuperWord", "-XX:-OptimizeFill"/*,
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintAssembly", "-XX:CompileCommand=compileonly,*SimdBenchmark"*/})
    public int[] bitWithoutSimd() {
        for (int i = 0; i < N; i++) {
            arrayC[i] = arrayB[i] & arrayA[i];
        }
        return arrayC;

    }

    @Benchmark
    @Fork(jvmArgs = {"-XX:+PrintGCDetails", "-server", "-XX:+UseSerialGC", "-Xmn8000M", "-Xms10000M", "-Xmx10000M", "-XX:+UseSuperWord", "-XX:+OptimizeFill"/*,
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintAssembly", "-XX:CompileCommand=compileonly,*SimdBenchmark"*/})
    public int bitAndSumWithSimd() {
        for (int i = 0; i < N; i++) {
            arrayC[i] = arrayB[i] & arrayA[i];
        }

        int value = Integer.MAX_VALUE;
        for (int i = 0; i < N; i++) {
            value &= arrayC[i];
        }
        return value;
    }

    @Benchmark
    @Fork(jvmArgs = {"-XX:+PrintGCDetails", "-server", "-XX:+UseSerialGC", "-Xmn8000M", "-Xms10000M", "-Xmx10000M", "-XX:-UseSuperWord", "-XX:-OptimizeFill"/*,
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintAssembly", "-XX:CompileCommand=compileonly,*SimdBenchmark"*/})
    public int bitAndSumWithoutSimd() {
        for (int i = 0; i < N; i++) {
            arrayC[i] = arrayB[i] & arrayA[i];
        }

        int value = Integer.MAX_VALUE;
        for (int i = 0; i < N; i++) {
            value &= arrayC[i];
        }
        return value;
    }

    @Benchmark
    @Fork(jvmArgs = {"-XX:+PrintGCDetails", "-server", "-XX:+UseSerialGC", "-Xmn8000M", "-Xms10000M", "-Xmx10000M", "-XX:+UseSuperWord", "-XX:+OptimizeFill"/*,
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintAssembly", "-XX:CompileCommand=compileonly,*SimdBenchmark"*/})
    public int bitAndSumSingleLoopWithSimd() {
        int value = Integer.MAX_VALUE;
        for (int i = 0; i < N; i++) {
            value &= arrayA[i] & arrayB[i];
        }
        return value;
    }

    @Benchmark
    @Fork(jvmArgs = {"-XX:+PrintGCDetails", "-server", "-XX:+UseSerialGC", "-Xmn8000M", "-Xms10000M", "-Xmx10000M", "-XX:-UseSuperWord", "-XX:-OptimizeFill"/*,
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintAssembly", "-XX:CompileCommand=compileonly,*SimdBenchmark"*/})
    public int bitAndSumSingleLoopWithoutSimd() {
        int value = Integer.MAX_VALUE;
        for (int i = 0; i < N; i++) {
            value &= arrayA[i] & arrayB[i];
        }
        return value;

    }

}
