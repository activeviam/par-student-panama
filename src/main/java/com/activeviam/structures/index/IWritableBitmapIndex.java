package com.activeviam.structures.index;

/**
 * @author ActiveViam
 */
public interface IWritableBitmapIndex extends IBitmapIndex {

	/**
	 * Appends a tuple in the index.
	 * @param tuple tuple to consider
	 * @return new size of this index
	 */
	int append(final int[] tuple);

	/**
	 * Cleans up the bitmap from newSize to the end of the bitmap.
	 * @param newSize the new size of the bitmap
	 */
	void truncate(final int newSize);

}
