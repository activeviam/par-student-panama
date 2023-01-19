package com.activeviam.benchmark.allocator;

import com.activeviam.Types;
import com.activeviam.chunk.DirectMemoryAllocator;
import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
public class JmhBenchmarkDirectMemoryAllocator extends JmhBenchmarkMemoryAllocator {
    @Setup
    public void setupAllocator() {
        ALLOCATOR = new DirectMemoryAllocator().getVectorAllocator(Types.INTEGER);
    }
}
