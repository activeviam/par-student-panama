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
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

/**
 * Abstract implementation for benchmarking the type-specific methods of the IVector interface.
 */
public abstract class AJmhBenchmarkTypedVector extends AJmhBenchmarkVector {

	protected abstract Types getVectorType();
	protected abstract IChunkAllocator createChunkAllocator();

	/**
	 * Creates the vector allocator for tested vectors.
	 */
	@Setup(Level.Trial)
	public void setupVectorAllocator() {
		final IChunkAllocator chunkAllocator = createChunkAllocator();
		VECTOR_ALLOCATOR = chunkAllocator.getVectorAllocator(getVectorType());
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
