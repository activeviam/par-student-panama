/*
 * (C) ActiveViam 2021
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.benchmark.vector;

import com.activeviam.Types;
import com.activeviam.vector.AFixedBlockVector;
import com.activeviam.vector.IVector;
import com.activeviam.vector.IVectorAllocator;
import java.math.BigDecimal;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Abstract for measuring the performances of the IVector interface's implementations.
 */
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 4, time = 100, timeUnit = MILLISECONDS)
@Measurement(iterations = 10, time = 100, timeUnit = MILLISECONDS)
@Fork(1)
public class AJmhBenchmarkVector {

	protected static final int cstValue = 1;

	/**
	 * Size of the tested vector.
	 */
	@Param({"1000"/*, "100000"*//* , "10000000" *//* ,"100000000" */})
	protected static int VECTOR_SIZE;

	/**
	 * Parameter for the content of the vector.
	 * <ul>
	 * <li>random is contains random values picked for the vector values type.
	 * <li>sparse contains 90% of a single value and random other values for the 10 remaining percent.
	 * <li>constant contains a single value everywhere.
	 * </ul>
	 */
	@Param({"random", "constant", "sparse"})
	protected static String VECTOR_CONTENT;

	protected static int HALF_VECTOR_SIZE;

	protected static final int BATCH_SIZE = 1000;

	protected static final int POS_CST = 3;

	protected static int[] POS_VALUES;

	/**
	 * Created an array of random positions that will be used when performing bencharking on random positions.
	 */
	@Setup(Level.Trial)
	public void initializePosition() {
		POS_VALUES = new int[BATCH_SIZE];
		for (int i = 0; i < BATCH_SIZE; i++) {
			POS_VALUES[i] = new Random().nextInt(VECTOR_SIZE);
		}
	}

	@Setup(Level.Trial)
	public void setupHalfSize() {
		HALF_VECTOR_SIZE = VECTOR_SIZE / 2;
	}

	/**
	 * Vector allocator instance used for this benchmark.
	 */
	protected static IVectorAllocator VECTOR_ALLOCATOR;

	/**
	 * Vector wrapper class.
	 */
	@State(Scope.Benchmark)
	public static class BenchmarkVector {

		protected IVector vector;

		/**
		 * Creates the tested vector.
		 */
		@Setup(Level.Trial)
		public void initializeVector() {
			this.vector = VECTOR_ALLOCATOR.allocateNewVector(VECTOR_SIZE);
			for (int i = 0; i < VECTOR_SIZE; i++) {
				this.vector.write(i, computeValue(VECTOR_ALLOCATOR.getComponentType(), VECTOR_CONTENT, cstValue));
			}
		}

		/**
		 * Destroys the tested vector.
		 */
		@TearDown(Level.Trial)
		public void teardownVector() {
			if (this.vector instanceof AFixedBlockVector) {
				((AFixedBlockVector) this.vector).release();
			}
		}

	}

	public static Object computeValue(Types contentType, String contentRepartition, double defaultValue) {
		BigDecimal value;
		switch (contentRepartition) {
			case "random" -> value = BigDecimal.valueOf(Integer.MAX_VALUE * Math.random());
			case "sparse" -> {
				if (Math.random() > 0.9) {
					value = BigDecimal.valueOf(Integer.MAX_VALUE * Math.random());
				} else {
					value = BigDecimal.valueOf(defaultValue);
				}
			}
			case "constant" -> value = BigDecimal.valueOf(defaultValue);
			default -> throw new IllegalStateException("Unexpected contentRepartition parameter value.");
		}
		switch (contentType) {
			case DOUBLE -> {
				return value.doubleValue();
			}
			case INTEGER -> {
				return value.intValue();
			}
			default -> throw new IllegalStateException(
					"Data creation not implemented for datatype : " + contentType + " , " + "which is : "
							+ contentType.name());
		}
	}

}
