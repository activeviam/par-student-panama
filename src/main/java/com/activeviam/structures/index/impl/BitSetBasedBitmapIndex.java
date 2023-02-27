package com.activeviam.structures.index.impl;

import com.activeviam.structures.bitmap.impl.BitSetBitmap;
import com.activeviam.structures.index.ABitmapIndex;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Bitmap indexed based on {@link BitSetBitmap}.
 *
 * @author ActiveViam
 */
public class BitSetBasedBitmapIndex extends ABitmapIndex<BitSetBitmap> {

	/**
	 * Constructor
	 *
	 * @param levels The number of indexed levels
	 */
	public BitSetBasedBitmapIndex(final int levels) {
		super(levels);
	}

	@Override
	protected BitSetBitmap[] createBitmapArray(final int length) {
		return new BitSetBitmap[length];
	}

	@Override
	protected BitSetBitmap createBitmap() {
		return new BitSetBitmap();
	}

	@Override
	protected BitSetBitmap createOnesBitmap(final int size) {
		final BitSet bitSet = new BitSet(size);
		bitSet.set(0, size);
		return new BitSetBitmap(bitSet);
	}

	@Override
	public void truncate(final int newSize) {
		size = newSize;
		Arrays.stream(this.index).forEach(index -> index.truncate(newSize));
	}
}
