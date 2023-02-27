package com.activeviam.structures.index.impl;

/**
 * Secondary index based on {@link BitSetBasedBitmapIndex}.
 *
 * @author ActiveViam
 */
public class BitSetBasedAppendOnlySecondaryIndex extends AAppendOnlySecondaryIndex {

	public BitSetBasedAppendOnlySecondaryIndex(int[] indexedFields) {
		super(indexedFields, new BitSetBasedBitmapIndex(indexedFields.length));
	}

	@Override
	public void discardBefore(long epoch, long[][] versions, int[] deletedRows) {
		System.out.println("discardBefore is not supported on " + getClass().getSimpleName());
	}

}
