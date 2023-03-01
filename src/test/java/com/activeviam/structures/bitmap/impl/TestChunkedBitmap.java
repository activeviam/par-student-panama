package com.activeviam.structures.bitmap.impl;

/**
 * Tests on {@link ChunkedBitmap}.
 *
 * @author ActiveViam
 */
public class TestChunkedBitmap extends ATestBitmap<ChunkedBitmap> {

	@Override
	protected ChunkedBitmap createBitmap() {
		return new ChunkedBitmap(128);
	}

}
