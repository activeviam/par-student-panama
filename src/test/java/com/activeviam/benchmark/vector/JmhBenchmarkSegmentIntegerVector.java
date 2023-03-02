/*
 * (C) ActiveViam 2021
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.benchmark.vector;

import com.activeviam.Types;
import com.activeviam.chunk.*;
import com.activeviam.vector.IVector;
import com.activeviam.vector.SegmentIntegerVector;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.foreign.MemorySession;

import static com.activeviam.Types.INTEGER;

/**
 * JMH Micro Benchmark for vector DOUBLE operation performances.
 */
@State(Scope.Benchmark)
public class JmhBenchmarkSegmentIntegerVector extends AJmhBenchmarkTypedVector {

	protected static int[] HALF_ARRAY;

	protected static final double quantile = 0.5;

	protected static final int writtenValue = 1;

	protected static IVector ZERO_VECTOR;
	
	@Setup(Level.Trial)
	@Override
	public void setupVectorAllocator() {
		VECTOR_ALLOCATOR = new SegmentMemoryAllocator(MemorySession.openConfined()).getVectorAllocator(INTEGER);
	}
	
	@Override
	protected IChunkAllocator createChunkAllocator() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Setup method for a vector containing only zero values that will be used for the stability of operations.
	 */
	@Setup(Level.Trial)
	public void setZeroVector() {
		ZERO_VECTOR = VECTOR_ALLOCATOR.allocateNewVector(VECTOR_SIZE);
		for (int i = 0; i < VECTOR_SIZE; i++) {
			ZERO_VECTOR.writeInt(i, 0);
		}
	}
	
	/**
	 * Setup method for a vector sized as half of the benched vector.
	 */
	@Setup(Level.Trial)
	public void setupHalfArray() {
		HALF_ARRAY = new int[HALF_VECTOR_SIZE];
		for (int i = 0; i < HALF_VECTOR_SIZE; i++) {
			HALF_ARRAY[i] = (int) computeValue(getVectorType(), "random", 0d);
		}
	}

	@Override
	protected Types getVectorType() {
		return INTEGER;
	}

	@Benchmark
	public void copyFromHalf(BenchmarkVector vector) {
		vector.vector.copyFrom(HALF_ARRAY);
	}

	@Benchmark
	public void copyToHalf(BenchmarkVector vector) {
		vector.vector.copyTo(HALF_ARRAY);
	}

	@Benchmark
	public void quantileInt(BenchmarkVector vector) {
		vector.vector.quantileInt(quantile);
	}
	
	@Benchmark
	public void topK(BenchmarkVector vector, Blackhole blackhole) {
		blackhole.consume(vector.vector.topK(VECTOR_SIZE / 5));
	}
	
	@Benchmark
	public void quickTopK(BenchmarkVector vector, Blackhole blackhole) {
		blackhole.consume(((SegmentIntegerVector) vector.vector).quickTopK(VECTOR_SIZE / 5));
	}
	
	@Benchmark
	public void quickTopKLomuto(BenchmarkVector vector, Blackhole blackhole) {
		blackhole.consume(((SegmentIntegerVector) vector.vector).quickTopKLomuto(VECTOR_SIZE / 5));
	}
	
	@Benchmark
	public void quickTopKSimd(BenchmarkVector vector, Blackhole blackhole) {
		blackhole.consume(((SegmentIntegerVector) vector.vector).quickTopKSimd(VECTOR_SIZE / 5));
	}
	
	@Benchmark
	public void quickTopKSimdFewAllocs(BenchmarkVector vector, Blackhole blackhole) {
		blackhole.consume(((SegmentIntegerVector) vector.vector).quickTopKSimdFewAllocs(VECTOR_SIZE / 5));
	}
	
	@Benchmark
	public void quickTopKNative(BenchmarkVector vector, Blackhole blackhole) {
		blackhole.consume(((SegmentIntegerVector) vector.vector).quickTopKNative(VECTOR_SIZE / 5));
	}

	@Benchmark
	public void sumInt(BenchmarkVector vector, Blackhole blackhole) {
		blackhole.consume(vector.vector.sumInt());
	}
	
	@Benchmark
	public void sumIntSimd(BenchmarkVector vector, Blackhole blackhole) {
		blackhole.consume(((SegmentIntegerVector) vector.vector).sumIntSimd());
	}

	@Benchmark
	public void toIntArray(BenchmarkVector vector) {
		vector.vector.toIntArray();
	}
	
	@Benchmark
	public void toIntArraySimd(BenchmarkVector vector) {
		((SegmentIntegerVector) vector.vector).toIntArraySimd();
	}

	/**
	 * Benchmark method for reading a constant position in a vector.
	 */
	@Benchmark
	public void readIntCst(BenchmarkVector vector, Blackhole blackHole) {
		for (int i = 0; i < BATCH_SIZE; i++) {
			blackHole.consume(vector.vector.readInt(POS_CST));
		}
	}

	/**
	 * Benchmark method for reading sequential positions in a vector.
	 */
	@Benchmark
	public void readIntSeq(BenchmarkVector vector, Blackhole blackHole) {
		for (int i = 0; i < BATCH_SIZE; i++) {
			blackHole.consume(vector.vector.readInt(i));
		}
	}

	/**
	 * Benchmark method for reading random positions in a vector.
	 */
	@Benchmark
	public void readIntRandom(BenchmarkVector vector, Blackhole blackHole) {
		for (int i = 0; i < BATCH_SIZE; i++) {
			blackHole.consume(vector.vector.readInt(POS_VALUES[i]));
		}
	}

	/**
	 * Benchmark method for writing in a constant position in a vector.
	 */
	@Benchmark
	public void writeIntCst(BenchmarkVector vector) {
		for (int i = 0; i < BATCH_SIZE; i++) {
			vector.vector.writeInt(POS_CST, writtenValue);
		}
	}

	/**
	 * Benchmark method for writing into sequential positions in a vector.
	 */
	@Benchmark
	public void writeIntSeq(BenchmarkVector vector) {
		for (int i = 0; i < BATCH_SIZE; i++) {
			vector.vector.writeInt(i, writtenValue);
		}
	}

	/**
	 * Benchmark method for reading into random positions in a vector.
	 */
	@Benchmark
	public void writeIntRandom(BenchmarkVector vector) {
		for (int i = 0; i < BATCH_SIZE; i++) {
			vector.vector.writeInt(POS_VALUES[i], writtenValue);
		}
	}

}
