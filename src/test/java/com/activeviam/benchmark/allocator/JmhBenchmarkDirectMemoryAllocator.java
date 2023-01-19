package com.activeviam.benchmark.allocator;

import com.activeviam.Types;
import com.activeviam.chunk.DirectMemoryAllocator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;

public class JmhBenchmarkDirectMemoryAllocator extends JmhBenchmarkMemoryAllocator {
    @Setup
    public void setupAllocator() {
        ALLOCATOR = new DirectMemoryAllocator();
    }

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

    @Param({"1000", "10000", "100000" })
    protected static int NB_ALLOCATIONS;

    @Benchmark
    public void allocateMultipleIntegerChunk() {
        for (int i = 0; i < NB_ALLOCATIONS; i++) {
            ALLOCATOR.allocateIntegerChunk(1);
        }
    }

    @Benchmark
    public void allocateMultipleDoubleChunk() {
        for (int i = 0; i < NB_ALLOCATIONS; i++) {
            ALLOCATOR.allocateDoubleChunk(1);
        }
    }

    @Benchmark
    public void allocateMultipleVectorChunk() {
        for (int i = 0; i < NB_ALLOCATIONS; i++) {
            ALLOCATOR.allocateVectorChunk(1, Types.INTEGER);
        }
    }

}
