/*
 * (C) ActiveViam 2021
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.benchmark.vector;

import com.activeviam.chunk.DirectMemoryAllocator;
import com.activeviam.chunk.IChunkAllocator;
import com.activeviam.vector.ITransientVector;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * JMH Micro Benchmark for vector SORTING performances.
 */
@State(Scope.Benchmark)
public class JmhBenchmarkVectorSorting extends AJmhBenchmarkUntypedVector {

	/** Benchmarks the sort operation on a vector. */
	@Benchmark
	public ITransientVector sortVector(BenchmarkVector vector) {
		return vector.vector.sort();
	}

	@Override
	protected IChunkAllocator createChunkAllocator() {
		return new DirectMemoryAllocator();
	}
}
