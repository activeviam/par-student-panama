/*
 * (C) ActiveViam 2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.allocator;

/**
 * Allocation type describing where data was allocated.
 *
 * @author ActiveViam
 */
public enum AllocationType {

	/**
	 * The data is allocated on the heap.
	 */
	ON_HEAP,

	/**
	 * The data is allocated on the direct memory.
	 */
	DIRECT,
	
	/**
	 * The data is allocated off-heap, using the MemorySegment API.
	 */
	SEGMENT,

}
