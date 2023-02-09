package com.activeviam.benchmark.allocator;

import com.activeviam.Types;
import com.activeviam.chunk.SegmentMemoryAllocator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

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
        ALLOCATOR = new SegmentMemoryAllocator(SESSION).getVectorAllocator(Types.INTEGER);
        BLACKHOLE = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
    }
    @TearDown(Level.Iteration)
    public void freeMemory() {
        SESSION.close();
        setupAllocator();
    }
}
