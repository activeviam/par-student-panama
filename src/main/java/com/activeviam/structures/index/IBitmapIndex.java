package com.activeviam.structures.index;

import com.activeviam.structures.bitmap.IBitmap;

/**
 * @author ActiveViam
 */
public interface IBitmapIndex {

	/** The ANY value */
	int ANY = -1;

	IBitmap matchBitmap(final int[] pattern);

	IBitmap matchBitmap(final int[][] compositePattern);

	/**
	 * Retrieves the bitmap stored at that position of that level.
	 *
	 * @param level A level
	 * @param value The position in the level
	 * @return the stored bitmap or {@code null}
	 */
	IBitmap getBitmap(final int level, final int value);

	/**
	 * Gets the number of indexed tuples this object can match.
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
