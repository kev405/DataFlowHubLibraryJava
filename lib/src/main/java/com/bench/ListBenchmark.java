package com.bench;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import com.entity.JobExecutionStub;

/**
 * Micro-benchmark comparing ArrayList and LinkedList
 * on typical operations with one million elements.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class ListBenchmark {
    
    private static final int ELEMENTS = 1_000_000;
    private List<JobExecutionStub> arrayList;
    private List<JobExecutionStub> linkedList;

    @Setup(Level.Iteration)
    public void setup() {
        arrayList  = new ArrayList<>(ELEMENTS);
        linkedList = new LinkedList<>();
        for (int i = 0; i < ELEMENTS; i++) {
            JobExecutionStub stub = new JobExecutionStub(i, "payload");
            arrayList.add(stub);
            linkedList.add(stub);
        }
    }

    /* ---------- add last ------------------------------------------------- */
    @Benchmark
    public void array_addLast() { arrayList.add(new JobExecutionStub(-1,"x")); }

    @Benchmark
    public void linked_addLast() { linkedList.add(new JobExecutionStub(-1,"x")); }

    /* ---------- add first ------------------------------------------------ */
    @Benchmark
    public void array_addFirst() { arrayList.add(0, new JobExecutionStub(-1,"x")); }

    @Benchmark
    public void linked_addFirst() { linkedList.add(0, new JobExecutionStub(-1,"x")); }

    /* ---------- random get ---------------------------------------------- */
    @Benchmark
    public JobExecutionStub array_randomGet() {
        int i = ThreadLocalRandom.current().nextInt(ELEMENTS);
        return arrayList.get(i);
    }

    @Benchmark
    public JobExecutionStub linked_randomGet() {
        int i = ThreadLocalRandom.current().nextInt(ELEMENTS);
        return linkedList.get(i);
    }

    /* ---------- full iteration ------------------------------------------ */
    @Benchmark
    public int array_iterate() {
        int sum = 0;
        for (JobExecutionStub stub : arrayList) sum += stub.idx();
        return sum;
    }

    @Benchmark
    public int linked_iterate() {
        int sum = 0;
        for (JobExecutionStub stub : linkedList) sum += stub.idx();
        return sum;
    }

}
