package com.activeviam.structures.index;

import com.activeviam.structures.bitmap.IBitmap;

public interface IColumnImprintsIndex {

	/** The ANY value */
	static final int ANY = -1;

	/**
	 * Gets the rows of the records matching the pattern as a bitmap.
	 *
	 * @param pattern The pattern to match
	 * @param size The size of the table
	 * @return the bitmap indicating the rows of the records matching the pattern
	 */
	IBitmap matchBitmap(final int[] pattern, final int size);

	/**
	 * Gets the rows of the records matching the composite pattern as a bitmap.
	 *
	 * @param compositePattern The composite pattern to match
	 * @param size The size of the table
	 * @return the bitmap indicating the rows of the records matching the composite pattern
	 */
	IBitmap matchBitmap(final int[][] compositePattern, final int size);

	/**
	 * Gets the number of indexed tuples this object can match.
	 *
	 * @return the number of tuples
	 */
	int size();

	/**
	 * Gets an estimation of the total size of the index.
	 *
	 * <p>
	 *   This includes data as well as abject internal attributes, class pointers, ...
	 * </p>
	 *
	 * @return the size of this index measured in bytes.
	 */
	long sizeInBytes();
}
