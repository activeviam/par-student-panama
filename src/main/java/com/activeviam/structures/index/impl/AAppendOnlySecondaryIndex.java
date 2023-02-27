package com.activeviam.structures.index.impl;

import com.activeviam.structures.bitmap.IBitmap;
import com.activeviam.structures.index.ISecondaryIndexBase;
import com.activeviam.structures.index.IWritableBitmapIndex;
import com.activeviam.structures.store.IRecord;

/**
 * @author ActiveViam
 */
public abstract class AAppendOnlySecondaryIndex implements ISecondaryIndexBase {

	/** The fields of the {@link IRecord} to index */
	protected final int[] indexedFields;

	protected final IWritableBitmapIndex index;

	/** The number of indexed records */
	protected int size = 0;

	public AAppendOnlySecondaryIndex(int[] indexedFields, IWritableBitmapIndex index) {
		this.indexedFields = indexedFields;
		this.index = index;
	}

	@Override
	public IBitmap getRows(int[] pattern) {
		return index.matchBitmap(pattern);
	}

	@Override
	public IBitmap getRows(int[][] compositePattern) {
		return index.matchBitmap(compositePattern);
	}

	@Override
	public long sizeInBytes() {
		// 16: Object header
		// 4:  Size attribute
		// 4:  Reference to the bitmap index
		long sizeInBytes = 16 + 8 + 4;
		sizeInBytes += index.sizeInBytes();
		// 4:  Reference to indexedFields
		// 12: Header of indexedFields
		sizeInBytes += 4 + 12;
		sizeInBytes += 4 * indexedFields.length;
		return sizeInBytes;
	}

	@Override
	public void index(IRecord record, int value) {
		if (value != size) {
			throw new UnsupportedOperationException("Not supported by this implementation: " + size + " " + value);
		}
		index.append(extractPoint(record));
		++size;
	}

	@Override
	public int[] extractPoint(IRecord record) {
		final int[] point = new int[indexedFields.length];
		for (int i = 0; i < point.length; ++i) {
			point[i] = record.readInt(indexedFields[i]);
		}
		return point;
	}

	@Override
	public void remove(IRecord record, int row) {
		throw new UnsupportedOperationException("Append only");
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public void truncate(int row) {
		index.truncate(row);
	}
}
