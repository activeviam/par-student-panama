/*
 * (C) ActiveViam 2021
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.benchmark.vector;

import com.activeviam.chunk.DirectMemoryAllocator;
import com.activeviam.chunk.IChunkAllocator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * JMH Micro Benchmark for vector STATS performances.
 */
@State(Scope.Benchmark)
public class JmhBenchmarkVectorStatistics extends AJmhBenchmarkUntypedVector {

	/** Benchmarks the average computation on a vector. */
	@Benchmark
	public double averageVector(BenchmarkVector vector) {
		return vector.vector.average();
	}

	/** Benchmarks the variance computation on a vector. */
	@Benchmark
	public double varianceVector(BenchmarkVector vector) {
		return vector.vector.variance();
	}

	@Override
	protected IChunkAllocator createChunkAllocator() {
		return new DirectMemoryAllocator();
	}
}
