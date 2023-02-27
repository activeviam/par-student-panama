package com.activeviam.structures.store;

import com.activeviam.structures.IDiscardable;
import com.activeviam.structures.bitmap.IBitmap;

/**
 * An {@link ITable} where each record hold a version number (epoch).
 *
 * @author ActiveViam
 */
public interface IVersionedTable extends ITable, IDiscardable {

	/**
	 * Checks whether the given row exists and is visible at the given version.
	 *
	 * @param row The row to check
	 * @param epoch The epoch to check
	 * @param visibleSize The size of the table at the given epoch
	 * @return <code>true</code> is the row exists at the given epoch, <code>false</code> otherwise
	 */
	boolean exists(int row, long epoch, int visibleSize);

	/**
	 * Gets the rows for the bitmap existing a the given epoch.
	 *
	 * @param rows A set of rows
	 * @param epoch The epoch to check
	 * @param visibleSize The size of the table at the given epoch
	 * @return the rows among the given set existing a the given epoch
	 */
	IBitmap filter(IBitmap rows, long epoch, int visibleSize);

	/**
	 * Gets the number of existing rows at the given epoch.
	 *
	 * @param epoch The epoch to check
	 * @param visibleSize The size of the table at the given epoch
	 * @return the number of existing rows at the given epoch
	 */
	int getValidRecordCount(long epoch, int visibleSize);

	/**
	 * Gets the total size of the table.
	 * <p>
	 *   This includes data as well as abject internal attributes, class pointers, ...
	 * </p>
	 * @return estimated size (in bytes) of the table
	 */
	long sizeInBytes();

}
