package com.activeviam.structures.index;

import com.activeviam.structures.index.impl.BitSetBasedBitmapIndex;

/**
 * Tests on {@link BitSetBasedBitmapIndex}.
 *
 * @author ActiveViam
 */
public class TestBitSetBasedBitmapIndex extends ATestBitmapIndex<BitSetBasedBitmapIndex> {

	@Override
	protected BitSetBasedBitmapIndex createBitmapIndex(int levels) {
		return new BitSetBasedBitmapIndex(levels);
	}

}
