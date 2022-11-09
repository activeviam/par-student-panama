/*
 * (C) ActiveViam 2021
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.benchmark.vector;

import static com.activeviam.Types.DOUBLE;

import com.activeviam.Types;
import com.activeviam.chunk.DirectMemoryAllocator;
import com.activeviam.chunk.IChunkAllocator;
import com.activeviam.vector.IVector;
import java.util.function.DoubleBinaryOperator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH Micro Benchmark for vector DOUBLE operation performances.
 */
@State(Scope.Benchmark)
public class JmhBenchmarkDirectDoubleVector extends AJmhBenchmarkTypedVector {

	protected static double[] HALF_ARRAY;

	protected static final double quantile = 0.5;

	protected static final double writtenValue = 1d;

	protected static IVector ZERO_VECTOR;

	/**
	 * Setup method for a vector containing only zero values that will be used for the stability of operations.
	 */
	@Setup(Level.Trial)
	public void setZeroVector() {
		ZERO_VECTOR = VECTOR_ALLOCATOR.allocateNewVector(VECTOR_SIZE);
		for (int i = 0; i < VECTOR_SIZE; i++) {
			ZERO_VECTOR.writeDouble(i, 0d);
		}
	}

	protected static final DoubleBinaryOperator operator = Double::sum;

	/**
	 * Setup method for a vector sized as half of the benched vector.
	 */
	@Setup(Level.Trial)
	public void setupHalfArray() {
		HALF_ARRAY = new double[HALF_VECTOR_SIZE];
		for (int i = 0; i < HALF_VECTOR_SIZE; i++) {
			HALF_ARRAY[i] = (double) computeValue(getVectorType(), "random", 0d);
		}
	}

	@Override
	protected Types getVectorType() {
		return DOUBLE;
	}

	@Override
	protected IChunkAllocator createChunkAllocator() {
		return new DirectMemoryAllocator();
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
	public void quantileDouble(BenchmarkVector vector) {
		vector.vector.quantileDouble(quantile);
	}

	@Benchmark
	public void sumDouble(BenchmarkVector vector) {
		vector.vector.sumDouble();
	}

	@Benchmark
	public void toDoubleArray(BenchmarkVector vector) {
		vector.vector.toDoubleArray();
	}

	/**
	 * Benchmark method for reading a constant position in a vector.
	 */
	@Benchmark
	public void readDoubleCst(BenchmarkVector vector, Blackhole blackHole) {
		for (int i = 0; i < BATCH_SIZE; i++) {
			blackHole.consume(vector.vector.readDouble(POS_CST));
		}
	}

	/**
	 * Benchmark method for reading sequential positions in a vector.
	 */
	@Benchmark
	public void readDoubleSeq(BenchmarkVector vector, Blackhole blackHole) {
		for (int i = 0; i < BATCH_SIZE; i++) {
			blackHole.consume(vector.vector.readDouble(i));
		}
	}

	/**
	 * Benchmark method for reading random positions in a vector.
	 */
	@Benchmark
	public void readDoubleRandom(BenchmarkVector vector, Blackhole blackHole) {
		for (int i = 0; i < BATCH_SIZE; i++) {
			blackHole.consume(vector.vector.readDouble(POS_VALUES[i]));
		}
	}

	/**
	 * Benchmark method for writing in a constant position in a vector.
	 */
	@Benchmark
	public void writeDoubleCst(BenchmarkVector vector) {
		for (int i = 0; i < BATCH_SIZE; i++) {
			vector.vector.writeDouble(POS_CST, writtenValue);
		}
	}

	/**
	 * Benchmark method for writing into sequential positions in a vector.
	 */
	@Benchmark
	public void writeDoubleSeq(BenchmarkVector vector) {
		for (int i = 0; i < BATCH_SIZE; i++) {
			vector.vector.writeDouble(i, writtenValue);
		}
	}

	/**
	 * Benchmark method for reading into random positions in a vector.
	 */
	@Benchmark
	public void writeDoubleRandom(BenchmarkVector vector) {
		for (int i = 0; i < BATCH_SIZE; i++) {
			vector.vector.writeDouble(POS_VALUES[i], writtenValue);
		}
	}

}
