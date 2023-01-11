package com.activeviam.benchmark.allocator;

import com.activeviam.Types;
import com.activeviam.chunk.IChunkAllocator;
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
    protected IChunkAllocator ALLOCATOR;

    @Param({"1000", "10000", "100000" })
    protected static int CHUNK_SIZE;

    @Benchmark
    public void allocateIntegerChunk() {
        ALLOCATOR.allocateIntegerChunk(CHUNK_SIZE);
    }
    @Benchmark
    public void allocateDoubleChunk() {
        ALLOCATOR.allocateDoubleChunk(CHUNK_SIZE);
    }

    @Benchmark
    public void allocateDoubleVectorChunk() {
        ALLOCATOR.allocateVectorChunk(CHUNK_SIZE, Types.INTEGER);
    }
}
