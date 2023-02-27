package com.activeviam.structures.index.impl;

import com.activeviam.structures.bitmap.IBitmap;
import com.activeviam.structures.index.IWritableColumnImprintsIndex;
import com.activeviam.structures.store.IRecord;
import com.activeviam.structures.store.ITable;

public class ColumnImprintsAppendOnlySecondaryIndex  {

	/** The fields of the {@link IRecord} to index */
	protected final int[] indexedFields;

	/** The column imprints index */
	protected final IWritableColumnImprintsIndex index;

	/** The number of indexed records */
	protected int size;

	/**
	 * Constructor
	 *
	 * @param indexedFields The indexes of the fields to index
	 * @param base The base of the table to index
	 */
	public ColumnImprintsAppendOnlySecondaryIndex(int[] indexedFields, ITable base) {
		this.indexedFields = indexedFields;
		this.index = new ColumnImprintsIndex(indexedFields, base);
		this.size = 0;
	}

	public IBitmap getRows(int[] pattern, int visibleSize) {
		return index.matchBitmap(pattern, visibleSize);
	}

	public IBitmap getRows(int[][] compositePattern, int visibleSize) {
		return index.matchBitmap(compositePattern, visibleSize);
	}

	/**
	 * Gets the total size of the index.
	 *
	 * <p>
	 *   This includes data as well as object internal attributes, class pointers, ...
	 * </p>
	 *
	 * @return estimated size (in bytes) of the index
	 */
	public long sizeInBytes() {
		// 16: Object header
		// 4:  Size attribute
		// 4:  Reference to the column imprints index
		long sizeInBytes = 16 + 8 + 4;
		sizeInBytes += index.sizeInBytes();
		// 4:  Reference to indexedFields
		// 12: Header of indexedFields
		sizeInBytes += 4 + 12;
		sizeInBytes += 4 * indexedFields.length;
		return sizeInBytes;
	}

	public void index(IRecord record, int row) {
		if (row != size) {
			throw new UnsupportedOperationException("Not supported by this implementation");
		}
		size = index.append(extractPoint(record));
	}

	public int[] extractPoint(IRecord record) {
		final int[] point = new int[indexedFields.length];
		for (int i = 0; i < point.length; ++i) {
			point[i] = record.readInt(indexedFields[i]);
		}
		return point;
	}

	/**
	 * Rebuilds some index levels if necessary
	 */
	public void rebuild() {
		index.rebuild();
	}

	/**
	 * Truncates the index.
	 *
	 * @param newSize The new size of the index
	 * @param imprintVectorsSizes The number of imprint vectors of each level
	 * @param lastImprintVectors The last imprint vector of each level
	 * @param cachelineDictionariesSizes The number of cacheline dictionaries of each level
	 * @param lastCachelineDictionaries The last cacheline dictionary of each level
	 */
	public void truncate(final int newSize,
			final int[] imprintVectorsSizes,
			final long[] lastImprintVectors,
			final int[] cachelineDictionariesSizes,
			final int[] lastCachelineDictionaries) {
        index.truncate(newSize, imprintVectorsSizes, lastImprintVectors, cachelineDictionariesSizes, lastCachelineDictionaries);
    }

	/**
	 * Gets the size of the index.
	 *
	 * @return the number of indexed records
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Gets the number of indexed fields.
	 *
	 * @return the number of indexed fields
	 */
	public int getIndexedFieldsSize() {
		return indexedFields.length;
	}

	/**
	 * Gets the size of imprint vectors array of that level.
	 *
	 * @param level A level
	 * @return the number of imprint vectors
	 */
	public int imprintVectorsSize(final int level) {
		return ((ColumnImprintsIndex) index).imprintVectorsSize(level);
	}

	/**
	 * Gets the last imprint vector of that level.
	 *
	 * @param level A level
	 * @return the last imprint vector
	 */
	public long lastImprintVector(final int level) {
		return ((ColumnImprintsIndex) index).lastImprintVector(level);
	}

	/**
	 * Gets the size of cacheline dictionaries array of that level.
	 *
	 * @param level A level
	 * @return the number of cacheline dictionaries
	 */
	public int cachelineDictionariesSize(final int level) {
		return ((ColumnImprintsIndex) index).cachelineDictionariesSize(level);
	}

	/**
	 * Gets the last cacheline dictionary of that level.
	 *
	 * @param level A level
	 * @return the last cacheline dictionary
	 */
	public int lastCachelineDictionary(final int level) {
		return ((ColumnImprintsIndex) index).lastCachelineDictionary(level);
	}

}
