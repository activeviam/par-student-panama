package com.activeviam.benchmark.allocator;

import com.activeviam.chunk.DirectMemoryAllocator;
import org.openjdk.jmh.annotations.Setup;

public class JmhBenchmarkDirectMemoryAllocator extends JmhBenchmarkMemoryAllocator {
    @Setup
    public void setupAllocator() {
        ALLOCATOR = new DirectMemoryAllocator();
    }

}
