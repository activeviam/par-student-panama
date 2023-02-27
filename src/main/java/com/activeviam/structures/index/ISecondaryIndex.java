package com.activeviam.structures.index;

import com.activeviam.structures.bitmap.IBitmap;
import com.activeviam.structures.store.IRecord;

/**
 * Interface for secondary index.
 *
 * @author ActiveViam
 */
public interface ISecondaryIndex {

	/**
	 * Gets the rows of the records matching the pattern.
	 *
	 * @param pattern The pattern to match
	 * @return the rows of the records matching the pattern
	 */
	IBitmap getRows(final int[] pattern);

	/**
	 * Gets the rows of the records matching the composite pattern.
	 *
	 * @param compositePattern The composite pattern to match
	 * @return the rows of the records matching the composite pattern
	 */
	IBitmap getRows(final int[][] compositePattern);

	int[] extractPoint(IRecord record);

}
