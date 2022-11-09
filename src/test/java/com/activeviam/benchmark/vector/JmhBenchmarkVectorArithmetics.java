/*
 * (C) ActiveViam 2021
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.benchmark.vector;

import com.activeviam.benchmark.vector.AJmhBenchmarkVector.BenchmarkVector;
import com.activeviam.chunk.DirectMemoryAllocator;
import com.activeviam.chunk.IChunkAllocator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * JMH Micro Benchmark for vector ARITHMETIC performances.
 */
@State(Scope.Benchmark)
public class JmhBenchmarkVectorArithmetics extends AJmhBenchmarkUntypedVector {

	protected static int VALUE = 1;

	protected static int ZERO_VALUE = 0;

	/** Benchmarks the scale operation on a vector. */
	@Benchmark
	public void scaleVector(BenchmarkVector vector) {
		// Need to use 1 to keep scaling identical as the same vector is reused
		// There with be a pitfall if scaling impl gets "smarter"
		vector.vector.scale(VALUE);
	}

	/** Benchmarks the translate operation on a vector. */
	@Benchmark
	public void translateVector(BenchmarkVector vector) {
		// Need to use 0 to keep scaling identical as the same vector is reused
		// There wit be a pitfall if translate impl gets "smarter"
		vector.vector.translate(ZERO_VALUE);
	}

	@Override
	protected IChunkAllocator createChunkAllocator() {
		return new DirectMemoryAllocator();
	}
}
