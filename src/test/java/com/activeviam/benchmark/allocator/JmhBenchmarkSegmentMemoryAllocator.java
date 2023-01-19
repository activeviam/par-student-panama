package com.activeviam.benchmark.allocator;

import com.activeviam.Types;
import com.activeviam.chunk.SegmentMemoryAllocator;
import org.openjdk.jmh.annotations.*;

import java.lang.foreign.MemorySession;

/**
 * JMH Micro Benchmark for performance of memory allocator using SEGMENT
 */
@State(Scope.Benchmark)
public class JmhBenchmarkSegmentMemoryAllocator extends JmhBenchmarkMemoryAllocator{
    protected MemorySession SESSION;
    @Setup
    public void setupAllocator() {
        SESSION = MemorySession.openShared();
        ALLOCATOR = new SegmentMemoryAllocator(SESSION);
    }
    @TearDown
    public void freeMemory() {
        SESSION.close();
        setupAllocator();
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
