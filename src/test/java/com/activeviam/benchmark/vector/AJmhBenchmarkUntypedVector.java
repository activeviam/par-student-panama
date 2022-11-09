/*
 * (C) ActiveViam 2021
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.benchmark.vector;

import com.activeviam.Types;
import com.activeviam.chunk.IChunkAllocator;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

/**
 * Abstract implementation for benchmarking the non-type-specific methods of the IVector interface.
 */
public abstract class AJmhBenchmarkUntypedVector extends AJmhBenchmarkVector {

	protected abstract IChunkAllocator createChunkAllocator();

	/**
	 * PARAMETERS:
	 * <ul>
	 * <li>Types.DOUBLE = "DOUBLE"
	 * <li>Types.INTEGER = "INTEGER"
	 * </ul>
	 */
	@Param({"DOUBLE",/* , "12848" */})
	protected static String VECTOR_TYPE;

	/**
	 * Creates the vector allocator for tested vectors.
	 */
	@Setup(Level.Trial)
	public void setupVectorAllocator() {
		final IChunkAllocator chunkAllocator = createChunkAllocator();
		VECTOR_ALLOCATOR = chunkAllocator.getVectorAllocator(getVectorType(VECTOR_TYPE));
	}

	private Types getVectorType(String type) {
		return Types.valueOf(type.toUpperCase());
	}

	/**
	 * De-allocates the tested vectors and destroys the allocator.
	 */
	@TearDown(Level.Trial)
	public void teardownVectorAllocator() {
		VECTOR_ALLOCATOR.release();
		VECTOR_ALLOCATOR = null;
	}

}
