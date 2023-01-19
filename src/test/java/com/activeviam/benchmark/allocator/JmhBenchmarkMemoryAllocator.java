package com.activeviam.benchmark.allocator;

import com.activeviam.Types;
import com.activeviam.chunk.IChunkAllocator;
import com.activeviam.vector.IVectorAllocator;
import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * JMH Micro Benchmark for performance of memory allocator
 */
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 4, time = 100, timeUnit = MILLISECONDS)
@Measurement(iterations = 10, time = 100, timeUnit = MILLISECONDS)
@Fork(1)
public class JmhBenchmarkMemoryAllocator {
    protected IVectorAllocator ALLOCATOR;

    @Param({"1000", "10000", "100000" })
    protected static int NB_ALLOCATIONS;

    @Benchmark
    public void allocateMultipleVectorChunk() {
        for (int i = 0; i < NB_ALLOCATIONS; i++) {
            ALLOCATOR.allocateNewVector(1);
        }
    }

    @Benchmark
    public void allocateSizeVectorChunk() {
        ALLOCATOR.allocateNewVector(NB_ALLOCATIONS);
    }
}
