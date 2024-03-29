package com.activeviam.benchmark.allocator;

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
}
