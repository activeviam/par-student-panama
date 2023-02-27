package com.activeviam.structures.index.impl;

import com.activeviam.structures.bitmap.IBitmap;
import com.activeviam.structures.bitmap.impl.BitSetBitmap;
import com.activeviam.structures.index.AColumnImprintsIndex;
import com.activeviam.structures.store.ITable;
import java.util.BitSet;

/**
 * Column Imprints index
 */
public class ColumnImprintsIndex extends AColumnImprintsIndex {

	/**
	 * Constructor
	 *
	 * @param indexedFields The indexes of the fields to index
	 * @param base The base of the table to index
	 */
	public ColumnImprintsIndex(int[] indexedFields, ITable base) {
		super(indexedFields, base);
	}

	@Override
	protected IBitmap createBitmap(final int size) {
		return new BitSetBitmap(size);
	}

	@Override
	protected IBitmap createEmptyBitmap() {
		return new BitSetBitmap();
	}

	@Override
	protected IBitmap createOnesBitmap(final int size) {
		final BitSet bitSet = new BitSet(size);
		bitSet.set(0, size);
		return new BitSetBitmap(bitSet);
	}

	@Override
	protected IBitmap[] createBitmapArray(final int length) {
		return new BitSetBitmap[length];
	}

}
