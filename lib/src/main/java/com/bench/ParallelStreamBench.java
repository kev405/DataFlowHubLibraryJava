package com.bench;

import org.openjdk.jmh.annotations.*;

import com.entity.JobExecutionStubParallel;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class ParallelStreamBench {

    /* ---------- params -------------------------------------------------- */
    @Param({"10000000"})          // 10 M doubles
    int doubles;

    @Param({"100000"})            // 100 k JobExecution
    int jobs;

    double[]   arr;
    List<JobExecutionStubParallel> jobList;

    @Setup
    public void setup() {
        arr = new Random(42).doubles(doubles).toArray();

        jobList = IntStream.range(0, jobs)
                .mapToObj(i -> new JobExecutionStubParallel(i, i % 2 == 0))
                .toList();
    }

    /* ---------- bench 1: sum ------------------------------------------- */

    @Benchmark public double sum_sequential() {
        return DoubleStream.of(arr).sum();
    }
    @Benchmark public double sum_parallel() {
        return DoubleStream.of(arr).parallel().sum();
    }

    /* ---------- bench 2: map-reduce ------------------------------------ */

    @Benchmark public long mapReduce_sequential() {
        return jobList.stream()
               .filter(JobExecutionStubParallel::success)
               .mapToLong(JobExecutionStubParallel::id)
               .sum();
    }
    @Benchmark public long mapReduce_parallel() {
        return jobList.parallelStream()
               .filter(JobExecutionStubParallel::success)
               .mapToLong(JobExecutionStubParallel::id)
               .sum();
    }
}
