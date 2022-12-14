/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.UnsafeUtil;
import com.activeviam.allocator.MemoryAllocator;
import java.util.BitSet;
import java.util.logging.Logger;

public class DirectIntegerChunk extends AbstractDirectChunk implements IntegerChunk {


	/** Unsafe provider. */
	private static final sun.misc.Unsafe UNSAFE = UnsafeUtil.getUnsafe();

	/** The order of the size in bytes of an element. */
	private static final int ELEMENT_SIZE_ORDER = 2;

	public DirectIntegerChunk(final MemoryAllocator allocator, final int capacity) {
		super(allocator, capacity, computeBlockSize(capacity));
	}

	private static long computeBlockSize(final int capacity) {
		final var minSize = capacity << ELEMENT_SIZE_ORDER;
		if (minSize % MemoryAllocator.PAGE_SIZE == 0) {
			return minSize;
		} else {
			// Find the closest multiple of PAGE_SIZE
			final var size = ((minSize / MemoryAllocator.PAGE_SIZE) + 1) * MemoryAllocator.PAGE_SIZE;
			Logger.getLogger("chunk").warning("Wasting " + (size - minSize) + " bytes");
			return size;
		}
	}

	@Override
	public int readInt(int position) {
		assert 0 <= position && position < capacity();
		return UNSAFE.getInt(offset(position << ELEMENT_SIZE_ORDER));
	}

	@Override
	public void writeInt(int position, int value) {
		assert 0 <= position && position < capacity();
		UNSAFE.putInt(offset(position << ELEMENT_SIZE_ORDER), value);
	}

	@Override
	public BitSet findRows(int value, int limit) {
		assert limit <= capacity();

		BitSet result = null;
		long addr = this.ptr;
		for (int i = 0; i < limit; i++) {
			if (UNSAFE.getInt(addr) == value) {
				if (result == null) {
					result = new BitSet();
				}
				result.set(i);
			}

			addr += 1 << ELEMENT_SIZE_ORDER;
		}
		return result;
	}

}
