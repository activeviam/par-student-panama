/*
 * (C) ActiveViam 2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk.vector;

import com.activeviam.Types;
import com.activeviam.chunk.IChunkAllocator;
import com.activeviam.chunk.IVectorChunk;
import com.activeviam.chunk.OnHeapAllocator;
import org.junit.jupiter.api.BeforeAll;

/**
 * @author ActiveViam
 */
public class TestArrayIntegerVectorChunk implements SpecTestIntegerVector {

	private static IChunkAllocator ALLOCATOR;
	@BeforeAll
	static void createAllocator() {
		ALLOCATOR = new OnHeapAllocator();
	}

	@Override
	public IVectorChunk createChunk(int capacity) {
		return ALLOCATOR.allocateVectorChunk(capacity, Types.INTEGER);
	}
}
